package controllers.musicpimp

import cats.effect.IO
import com.malliina.concurrent.Execution.runtime
import com.malliina.musicpimp.html.{LoginContent, PimpHtml, UsersContent}
import com.malliina.musicpimp.models.NewUser
import com.malliina.play.PimpAuthenticator
import com.malliina.play.auth.{Auth, RememberMe}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.forms.FormMappings
import com.malliina.play.http.RequestHeaderOps
import com.malliina.values.Username
import controllers.musicpimp.Accounts.{UsersFeedback, log}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms.*
import play.api.mvc.*

object Accounts:
  private val log = Logger(getClass)

  val UsersFeedback = "usersFeedback"

class Accounts(tags: PimpHtml, auth: PimpAuthenticator, pimpAuth: AuthDeps, accs: AccountForms)
  extends HtmlController(pimpAuth):

  val userFormKey = accs.userFormKey
  val rememberMeKey = accs.rememberMeKey
  val passFormKey = accs.passFormKey
  val feedback = accs.feedback
  val userManager = auth.userManager
  val rememberMe = auth.rememberMe
  val invalidCredentialsMessage = "Invalid credentials."
  val defaultCredentialsMessage =
    s"Welcome! The default credentials of ${userManager.defaultUser} / ${userManager.defaultPass} have not been changed. " +
      s"Consider changing the password under the Manage tab once you have logged in."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."
  val incorrectPasswordMessage = "Incorrect password."
  val repeatPassFailureMessage = "The password was incorrectly repeated."
  val cannotDeleteYourself = "You cannot delete yourself."

  val rememberMeLoginForm = accs.rememberMeLoginForm

  val addUserForm = Form(
    mapping(
      userFormKey -> FormMappings.username,
      accs.newPassKey -> FormMappings.password,
      accs.newPassAgainKey -> FormMappings.password
    )(NewUser.apply)(u => Option((u.username, u.pass, u.passAgain)))
      .verifying(repeatPassFailureMessage, _.passwordsMatch)
  )

  def account = pimpAction: request =>
    Ok(tags.account(request.user, UserFeedback.flashed(request)))

  def users = pimpActionAsyncIO: request =>
    userManager.users.map(us => Ok(usersPage(us, addUserForm, request)))

  def delete(user: Username) = pimpActionAsyncIO: request =>
    val redir = Redirect(routes.Accounts.users)
    if user != request.user then
      userManager
        .deleteUser(user)
        .map: _ =>
          redir.flashing(UsersFeedback -> s"Deleted user '${user.name}'.")
    else
      IO.pure(
        redir
          .flashing(UsersFeedback -> cannotDeleteYourself, UserFeedback.Success -> UserFeedback.No)
      )

  def loginPage = Action.async: request =>
    userManager.isDefaultCredentials
      .map: isDefault =>
        val motd = if isDefault then Option(defaultCredentialsMessage) else None
        val flashFeedback = UserFeedback.flashed(request.flash, accs.feedback)
        Ok(tags.login(LoginContent(accs, motd, None, flashFeedback)))
      .unsafeToFuture()

  def logout = authAction: _ =>
    // TODO remove the cookie token series, otherwise it will just remain in storage, unused
    Redirect(routes.Accounts.loginPage).withNewSession
      .discardingCookies(RememberMe.discardingCookie)
      .flashing(UserFeedback.success(logoutMessage).flash)

  def formAddUser = pimpActionAsyncIO: request =>
    addUserForm
      .bindFromRequest()(request, formBinding)
      .fold(
        formWithErrors =>
          val user = formWithErrors.data.getOrElse(userFormKey, "")
          log.warn(
            s"Unable to add user '$user' from '${request.realAddress}', form: $formWithErrors"
          )
          userManager.users.map(users => BadRequest(usersPage(users, formWithErrors, request)))
        ,
        newUser =>
          val addCall = userManager.addUser(newUser.username, newUser.pass)
          addCall.map: addError =>
            val userFeedback = addError
              .map(e => UserFeedback.error(s"User '${e.user}' already exists."))
              .getOrElse(UserFeedback.success(s"Created user '${newUser.username}'."))
            Redirect(routes.Accounts.users).flashing(userFeedback.flash)
      )

  def usersPage(users: Seq[Username], form: Form[NewUser], req: PimpUserRequest) =
    val addFeedback =
      form.globalError
        .map(err => UserFeedback.error(err.message))
        .orElse(UserFeedback.flashed(req))
    val listFeedback = UserFeedback.flashed(req.flash, textKey = UsersFeedback)
    tags.users(UsersContent(users, req.user, listFeedback, addFeedback))

  def formAuthenticate = Action.async { request =>
    val remoteAddress = request.realAddress
    val flashFeedback = UserFeedback.flashed(request.flash, accs.feedback)
    rememberMeLoginForm
      .bindFromRequest()(request, formBinding)
      .fold(
        formWithErrors =>
          val user = formWithErrors.data.getOrElse(userFormKey, "")
          log warn s"Authentication failed for user: '$user' from '$remoteAddress'."
          val formFeedback = UserFeedback.formed(formWithErrors)
          fut(BadRequest(tags.login(LoginContent(accs, None, formFeedback, flashFeedback))))
        ,
        credentials =>
          val username = credentials.username
          auth
            .authenticate(username, credentials.password)
            .flatMap: isValid =>
              if isValid then
                log info s"Authentication succeeded for user '$username' from '$remoteAddress'."
                val intendedUrl: String =
                  request.session.get(accs.intendedUri).getOrElse(defaultLoginSuccessPage.url)
                val result =
                  Results.Redirect(intendedUrl).withSession(Auth.DefaultSessionKey -> username.name)
                if credentials.rememberMe then
                  log debug s"Remembering auth..."
                  // create token, retrieve cookie
                  val cookie = rememberMe.persistNewCookie(username)
                  cookie.map(c => result.withCookies(c))
                else IO.pure(result)
              else
                log.warn(s"Invalid form authentication for user '$username'.")
                val formFeedback = UserFeedback.error("Incorrect username or password.")
                IO.pure(
                  BadRequest(
                    tags.login(LoginContent(accs, None, Option(formFeedback), flashFeedback))
                  )
                )
            .unsafeToFuture()
      )
  }

  def defaultLoginSuccessPage: Call = routes.LibraryController.rootLibrary

  def formChangePassword = pimpActionAsyncIO: request =>
    val remoteAddress = request.realAddress
    val user = request.user
    accs.changePasswordForm
      .bindFromRequest()(request, formBinding)
      .fold(
        errors =>
          val feedback = UserFeedback.formed(errors)
          val msg = feedback.fold("")(m => s" ${m.message}")
          log warn s"Unable to change password for user '$user' from '$remoteAddress'.$msg"
          IO.pure(BadRequest(tags.account(user, feedback)))
        ,
        pc =>
          auth
            .authenticate(user, pc.oldPass)
            .flatMap: isValid =>
              if isValid then
                userManager
                  .updatePassword(user, pc.newPass)
                  .map: _ =>
                    log info s"Password changed for user '$user' from '$remoteAddress'."
                    Redirect(routes.Accounts.account)
                      .flashing(UserFeedback.success(passwordChangedMessage).flash)
              else
                IO.pure(
                  BadRequest(
                    tags.account(user, Option(UserFeedback.error(incorrectPasswordMessage)))
                  )
                )
      )
