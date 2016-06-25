package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.models.User
import com.malliina.play.PimpAuthenticator
import com.malliina.play.auth.RememberMe
import com.malliina.play.controllers.AccountController
import controllers.Accounts.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._
import views.html

import scala.concurrent.Future

object Accounts {
  private val log = Logger(getClass)

  val Feedback = "feedback"
  val UsersFeedback = "usersFeedback"
  val Success = "success"
  val IntendedUri = "intended_uri"
}

class Accounts(auth: PimpAuthenticator, mat: Materializer)
  extends HtmlController(auth, mat)
    with AccountController {

  val userManager = auth.userManager
  val rememberMe = auth.rememberMe
  val invalidCredentialsMessage = "Invalid credentials."
  val defaultCredentialsMessage = s"Welcome! The default credentials of ${userManager.defaultUser} / ${userManager.defaultPass} have not been changed. " +
    s"Consider changing the password under the Manage tab once you have logged in."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."

  val rememberMeLoginForm = Form(tuple(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText,
    rememberMeKey -> boolean // the checkbox HTML element must have the property 'value="true"'
  ))

  val addUserForm = Form(tuple(
    userFormKey -> nonEmptyText,
    newPassKey -> nonEmptyText,
    newPassAgainKey -> nonEmptyText
  ).verifying("The password was incorrectly repeated.", _ match {
    case (_, newPass, newPassAgain) => newPass == newPassAgain
  }))

  def account = pimpAction { request =>
    Ok(html.account(request.user, changePasswordForm(request))(request.flash))
  }

  def users = pimpActionAsync { request =>
    userManager.users.map(us => Ok(html.users(us, addUserForm)(request.flash)))
  }

  def delete(user: String) = pimpActionAsync { request =>
    val redir = Redirect(routes.Accounts.users())
    if (user != request.user) {
      (userManager deleteUser User(user)).map(_ => redir)
    } else {
      Future.successful(redir.flashing(Accounts.UsersFeedback -> "You cannot delete yourself."))
    }
  }

  def login = Action.async { request =>
    userManager.isDefaultCredentials.map { isDefault =>
      val motd = if (isDefault) Option(defaultCredentialsMessage) else None
      Ok(html.login(this, rememberMeLoginForm, motd)(request.flash))
    }
  }

  def logout = AuthAction { request =>
    // TODO remove the cookie token series, otherwise it will just remain in storage, unused
    Redirect(routes.Accounts.login())
      .withNewSession
      .discardingCookies(RememberMe.discardingCookie)
      .flashing(msg(logoutMessage))
  }

  def formAddUser = pimpActionAsync { request =>
    val remoteAddress = request.remoteAddress
    addUserForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Unable to add user: $user from: $remoteAddress, form: $formWithErrors"
        userManager.users.map(users => BadRequest(html.users(users, formWithErrors)(request.flash)))
      },
      credentials => {
        val (user, pass, _) = credentials
        val addCall = userManager.addUser(User(user), pass)
        addCall.map(addError => {
          val (isSuccess, feedback) = addError.fold((true, s"Created user $user."))(e => (false, s"User ${e.user} already exists."))
          Redirect(routes.Accounts.users()).flashing(msg(feedback), "success" -> (if (isSuccess) "yes" else "no"))
        })
      }
    )
  }

  def formAuthenticate = Action.async { request =>
    val remoteAddress = request.remoteAddress
    rememberMeLoginForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        Future.successful(BadRequest(html.login(this, formWithErrors)(request.flash)))
      },
      credentials => {
        val (user, pass, shouldRemember) = credentials
        validateCredentials(user, pass) flatMap { isValid =>
          if (isValid) {
            log info s"Authentication succeeded for user: $user from: $remoteAddress"
            val intendedUrl = request.session.get(INTENDED_URI).getOrElse(defaultLoginSuccessPage.url)
            val result = Redirect(intendedUrl).withSession(Security.username -> user)
            if (shouldRemember) {
              log debug s"Remembering auth..."
              // create token, retrieve cookie
              val cookie = rememberMe persistNewCookie user
              cookie.map(c => result.withCookies(c))
            } else {
              Future.successful(result)
            }
          } else {
            log.warn(s"Invalid form authentication for user $user")
            // TODO show an "authentication failed" message to the user
            Future.successful(Unauthorized)
          }
        }
      }
    )
  }

  def defaultLoginSuccessPage: Call = routes.LibraryController.rootLibrary()

  def formChangePassword = pimpActionAsync { request =>
    val user = request.user
    changePasswordForm(request).bindFromRequest()(request).fold(
      errors => {
        log warn s"Unable to change password for user: $user from: ${request.remoteAddress}, form: $errors"
        Future.successful(BadRequest(html.account(user, errors)(request.flash)))
      },
      success => {
        val (old, newPass, _) = success
        validateCredentials(user, old) flatMap { isValid =>
          if (isValid) {
            userManager.updatePassword(User(user), newPass) map { _ =>
              log info s"Password changed for user: $user from: ${request.remoteAddress}"
              Redirect(routes.Accounts.account()).flashing(msg(passwordChangedMessage))
            }
          } else {
            Future.successful(BadRequest(html.account(user, changePasswordForm(request))(request.flash)))
          }
        }
      }
    )
  }

  def msg(message: String) = FEEDBACK -> message

  override protected def logUnauthorized(implicit request: RequestHeader): Unit = super.logUnauthorized
}
