package controllers

import com.mle.musicpimp.auth.CookieLogin
import com.mle.play.auth.RememberMe
import com.mle.play.controllers.AccountController
import com.mle.util.Log
import play.api.data.Form
import play.api.data.Forms._
import play.api.mvc._
import views.html

/**
 * @author Michael
 */
object Accounts {
  val FEEDBACK = "feedback"
  val USERS_FEEDBACK = "usersFeedback"
  val SUCCESS = "success"
  val INTENDED_URI = "intended_uri"
}

class Accounts extends HtmlController with AccountController with Log {
  val invalidCredentialsMessage = "Invalid credentials."
  val defaultCredentialsMessage = s"Welcome! The default credentials of ${userManager.defaultUser} / ${userManager.defaultPass} have not been changed. " +
    s"Consider changing the password under the Manage tab once you have logged in."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."

  val rememberMeLoginForm = Form(tuple(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText,
    rememberMeKey -> boolean // the checkbox HTML element must have the property 'value="true"'
  ) verifying(invalidCredentialsMessage, _ match {
    case (username, password, _) => validateCredentials(username, password)
  }))

  val addUserForm = Form(tuple(
    userFormKey -> nonEmptyText,
    newPassKey -> nonEmptyText,
    newPassAgainKey -> nonEmptyText
  ).verifying("The password was incorrectly repeated.", _ match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))

  def account = PimpAction(implicit req => Ok(html.account(req.user, changePasswordForm)))

  def users = PimpAction(implicit req => Ok(html.users(userManager.users, addUserForm)))

  def delete(user: String) = PimpAction(implicit req => {
    val redir = Redirect(routes.Accounts.users())
    if (user != req.user) {
      userManager deleteUser user
      redir
    } else {
      redir.flashing(Accounts.USERS_FEEDBACK -> "You cannot delete yourself.")
    }
  })


  def login = Action(implicit request => {
    val motd = Option(defaultCredentialsMessage).filter(_ => userManager.isDefaultCredentials)
    Ok(html.login(this, rememberMeLoginForm, motd))
  })

  def logout = AuthAction(implicit request => {
    // TODO remove the cookie token series, otherwise it will just remain in storage, unused
    Redirect(routes.Accounts.login()).withNewSession.discardingCookies(RememberMe.discardingCookie)
      .flashing(msg(logoutMessage))
  })

  def formAddUser = PimpAction(implicit req => {
    val remoteAddress = req.remoteAddress
    addUserForm.bindFromRequest.fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Unable to add user: $user from: $remoteAddress, form: $formWithErrors"
        BadRequest(html.users(userManager.users, formWithErrors))
      },
      credentials => {
        val (user, pass, _) = credentials
        val addCall = userManager.addUser(user, pass)
        val (isSuccess, feedback) = addCall.fold((true, s"Created user $user."))(e => (false, s"User ${e.user} already exists."))
        Redirect(routes.Accounts.users()).flashing(msg(feedback), "success" -> (if (isSuccess) "yes" else "no"))
      }
    )
  })

  def formAuthenticate = Action(implicit request => {
    val remoteAddress = request.remoteAddress
    rememberMeLoginForm.bindFromRequest.fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        BadRequest(html.login(this, formWithErrors))
      },
      credentials => {
        val (user, _, shouldRemember) = credentials
        log info s"Authentication succeeded for user: $user from: $remoteAddress"
        val intendedUrl = request.session.get(INTENDED_URI).getOrElse(defaultLoginSuccessPage.url)
        val result = Redirect(intendedUrl).withSession(Security.username -> user)
        if (shouldRemember) {
          log debug s"Remembering auth..."
          // create token, retrieve cookie
          val cookie = CookieLogin persistNewCookie user
          result.withCookies(cookie)
        } else {
          result
        }
      }
    )
  })

  def defaultLoginSuccessPage: Call = routes.Website.rootLibrary()

  def formChangePassword = PimpAction(implicit request => {
    val user = request.user
    changePasswordForm.bindFromRequest.fold(
      errors => {
        log warn s"Unable to change password for user: $user from: ${request.remoteAddress}, form: $errors"
        BadRequest(html.account(user, errors))
      },
      success => {
        val (_, newPass, _) = success
        userManager updatePassword(user, newPass)
        log info s"Password changed for user: $user from: ${request.remoteAddress}"
        Redirect(routes.Accounts.account()).flashing(msg(passwordChangedMessage))
      }
    )
  })

  def msg(message: String) = FEEDBACK -> message

  override protected def logUnauthorized(implicit request: RequestHeader): Unit = super.logUnauthorized
}
