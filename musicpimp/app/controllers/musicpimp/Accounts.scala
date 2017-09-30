package controllers.musicpimp

import akka.stream.Materializer
import com.malliina.musicpimp.models.NewUser
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.play.PimpAuthenticator
import com.malliina.play.auth.{Auth, Authenticator, RememberMe}
import com.malliina.play.controllers.AccountForms
import com.malliina.play.http.{AuthedRequest, Proxies}
import com.malliina.play.models.{Password, Username}
import controllers.musicpimp.Accounts.{Success, UsersFeedback, log}
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._

object Accounts {
  private val log = Logger(getClass)

  val Feedback = "feedback"
  val UsersFeedback = "usersFeedback"
  val Success = "success"
}

class Accounts(tags: PimpHtml,
               auth: PimpAuthenticator,
               pimpAuth: AuthDeps,
               accs: AccountForms)
  extends HtmlController(pimpAuth) {

  val userFormKey = accs.userFormKey
  val rememberMeKey = accs.rememberMeKey
  val passFormKey = accs.passFormKey
  val feedback = accs.feedback
  val userManager = auth.userManager
  val rememberMe = auth.rememberMe
  val invalidCredentialsMessage = "Invalid credentials."
  val defaultCredentialsMessage = s"Welcome! The default credentials of ${userManager.defaultUser} / ${userManager.defaultPass} have not been changed. " +
    s"Consider changing the password under the Manage tab once you have logged in."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."
  val incorrectPasswordMessage = "Incorrect password."
  val repeatPassFailureMessage = "The password was incorrectly repeated."
  val cannotDeleteYourself = "You cannot delete yourself."

  val rememberMeLoginForm = accs.rememberMeLoginForm

  val addUserForm = Form(mapping(
    userFormKey -> Username.mapping,
    accs.newPassKey -> Password.mapping,
    accs.newPassAgainKey -> Password.mapping
  )(NewUser.apply)(NewUser.unapply)
    .verifying(repeatPassFailureMessage, _.passwordsMatch))

  def account = pimpAction { request =>
    Ok(tags.account(request.user, UserFeedback.flashed(request)))
  }

  def users = pimpActionAsync { request =>
    userManager.users.map(us => Ok(usersPage(us, addUserForm, request)))
  }

  def delete(user: Username) = pimpActionAsync { request =>
    val redir = Redirect(routes.Accounts.users())
    if (user != request.user) {
      (userManager deleteUser user) map { _ =>
        redir.flashing(UsersFeedback -> s"Deleted user '${user.name}'.")
      }
    } else {
      fut(redir.flashing(
        UsersFeedback -> cannotDeleteYourself,
        UserFeedback.Success -> UserFeedback.No))
    }
  }

  def login = Action.async { request =>
    userManager.isDefaultCredentials.map { isDefault =>
      val motd = if (isDefault) Option(defaultCredentialsMessage) else None
      val flashFeedback = UserFeedback.flashed(request.flash, accs.feedback)
      Ok(tags.login(accs, motd, None, flashFeedback))
    }
  }

  def logout = authAction { _ =>
    // TODO remove the cookie token series, otherwise it will just remain in storage, unused
    Redirect(routes.Accounts.login())
      .withNewSession
      .discardingCookies(RememberMe.discardingCookie)
      .flashing(msg(logoutMessage))
  }

  def formAddUser = pimpActionAsync { request =>
    val remoteAddress = Proxies.realAddress(request)
    addUserForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Unable to add user '$user' from '$remoteAddress', form: $formWithErrors"
        userManager.users.map(users => BadRequest(usersPage(users, formWithErrors, request)))
      },
      newUser => {
        val addCall = userManager.addUser(newUser.username, newUser.pass)
        addCall.map { addError =>
          val (isSuccess, feedback) = addError
            .map(e => (false, s"User '${e.user}' already exists."))
            .getOrElse((true, s"Created user '${newUser.username}'."))
          Redirect(routes.Accounts.users())
            .flashing(msg(feedback), Success -> (if (isSuccess) "yes" else "no"))
        }
      }
    )
  }

  def usersPage(users: Seq[Username], form: Form[NewUser], req: PimpUserRequest) = {
    val addFeedback =
      form.globalError.map(err => UserFeedback.error(err.message))
        .orElse(UserFeedback.flashed(req))
    val listFeedback = UserFeedback.flashed(req.flash, textKey = UsersFeedback)
    tags.users(users, req.user, listFeedback, addFeedback)
  }

  def formAuthenticate = Action.async { request =>
    val remoteAddress = Proxies.realAddress(request)
    val flashFeedback = UserFeedback.flashed(request.flash, accs.feedback)
    rememberMeLoginForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: '$user' from '$remoteAddress'."
        val formFeedback = UserFeedback.formed(formWithErrors)
        fut(BadRequest(tags.login(accs, None, formFeedback, flashFeedback)))
      },
      credentials => {
        val username = credentials.username
        auth.authenticate(username, credentials.password) flatMap { isValid =>
          if (isValid) {
            log info s"Authentication succeeded for user '$username' from '$remoteAddress'."
            val intendedUrl: String = request.session.get(accs.intendedUri).getOrElse(defaultLoginSuccessPage.url)
            val result = Results.Redirect(intendedUrl).withSession(Auth.DefaultSessionKey -> username.name)
            if (credentials.rememberMe) {
              log debug s"Remembering auth..."
              // create token, retrieve cookie
              val cookie = rememberMe persistNewCookie username
              cookie.map(c => result.withCookies(c))
            } else {
              fut(result)
            }
          } else {
            log.warn(s"Invalid form authentication for user '$username'.")
            val formFeedback = UserFeedback.error("Incorrect username or password.")
            fut(BadRequest(tags.login(accs, None, Option(formFeedback), flashFeedback)))
          }
        }
      }
    )
  }

  def defaultLoginSuccessPage: Call = routes.LibraryController.rootLibrary()

  def formChangePassword = pimpActionAsync { request =>
    val remoteAddress = Proxies.realAddress(request)
    val user = request.user
    accs.changePasswordForm.bindFromRequest()(request).fold(
      errors => {
        val feedback = UserFeedback.formed(errors)
        val msg = feedback.fold("")(m => s" ${m.message}")
        log warn s"Unable to change password for user '$user' from '$remoteAddress'.$msg"
        fut(BadRequest(tags.account(user, feedback)))
      },
      pc => {
        auth.authenticate(user, pc.oldPass) flatMap { isValid =>
          if (isValid) {
            userManager.updatePassword(user, pc.newPass) map { _ =>
              log info s"Password changed for user '$user' from '$remoteAddress'."
              Redirect(routes.Accounts.account())
                .flashing(msg(passwordChangedMessage))
            }
          } else {
            fut(BadRequest(tags.account(user, Option(UserFeedback.error(incorrectPasswordMessage)))))
          }
        }
      }
    )
  }

  def msg(message: String) = UserFeedback.Feedback -> message
}
