package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.models.NewUser
import com.malliina.play.PimpAuthenticator
import com.malliina.play.auth.RememberMe
import com.malliina.play.controllers.AccountForms
import com.malliina.play.models.{Password, Username}
import controllers.Accounts.log
import play.api.Logger
import play.api.data.Form
import play.api.data.Forms._
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

class Accounts(auth: PimpAuthenticator, mat: Materializer, accs: AccountForms)
  extends HtmlController(auth, mat) {

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

  val rememberMeLoginForm = accs.rememberMeLoginForm

  val addUserForm = Form(mapping(
    userFormKey -> Username.mapping,
    accs.newPassKey -> Password.mapping,
    accs.newPassAgainKey -> Password.mapping
  )(NewUser.apply)(NewUser.unapply)
    .verifying("The password was incorrectly repeated.", _.passwordsMatch))

  def account = pimpAction { request =>
    Ok(html.account(request.user, accs.changePasswordForm)(request.flash))
  }

  def users = pimpActionAsync { request =>
    userManager.users.map(us => Ok(html.users(us, addUserForm)(request.flash)))
  }

  def delete(user: Username) = pimpActionAsync { request =>
    val redir = Redirect(routes.Accounts.users())
    if (user != request.user) {
      (userManager deleteUser user).map(_ => redir)
    } else {
      Future.successful(redir.flashing(Accounts.UsersFeedback -> "You cannot delete yourself."))
    }
  }

  def login = Action.async { request =>
    userManager.isDefaultCredentials.map { isDefault =>
      val motd = if (isDefault) Option(defaultCredentialsMessage) else None
      Ok(html.login(accs, rememberMeLoginForm, motd)(request.flash))
    }
  }

  def logout = authAction { request =>
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
      newUser => {
        val addCall = userManager.addUser(newUser.username, newUser.pass)
        addCall.map { addError =>
          val (isSuccess, feedback) = addError.fold((true, s"Created user ${newUser.username}."))(e => (false, s"User ${e.user} already exists."))
          Redirect(routes.Accounts.users()).flashing(msg(feedback), "success" -> (if (isSuccess) "yes" else "no"))
        }
      }
    )
  }

  def formAuthenticate = Action.async { request =>
    val remoteAddress = request.remoteAddress
    rememberMeLoginForm.bindFromRequest()(request).fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        Future.successful(BadRequest(html.login(accs, formWithErrors)(request.flash)))
      },
      credentials => {
        val username = credentials.username
        validateCredentials(username, credentials.password) flatMap { isValid =>
          if (isValid) {
            log info s"Authentication succeeded for user: $username from: $remoteAddress"
            val intendedUrl = request.session.get(accs.intendedUri).getOrElse(defaultLoginSuccessPage.url)
            val result = Redirect(intendedUrl).withSession(Security.username -> username.name)
            if (credentials.rememberMe) {
              log debug s"Remembering auth..."
              // create token, retrieve cookie
              val cookie = rememberMe persistNewCookie username
              cookie.map(c => result.withCookies(c))
            } else {
              Future.successful(result)
            }
          } else {
            log.warn(s"Invalid form authentication for user $username")
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
    accs.changePasswordForm.bindFromRequest()(request).fold(
      errors => {
        log warn s"Unable to change password for user: $user from: ${request.remoteAddress}, form: $errors"
        Future.successful(BadRequest(html.account(user, errors)(request.flash)))
      },
      pc => {
        validateCredentials(user, pc.oldPass) flatMap { isValid =>
          if (isValid) {
            userManager.updatePassword(user, pc.newPass) map { _ =>
              log info s"Password changed for user: $user from: ${request.remoteAddress}"
              Redirect(routes.Accounts.account()).flashing(msg(passwordChangedMessage))
            }
          } else {
            Future.successful(BadRequest(html.account(user, accs.changePasswordForm)(request.flash)))
          }
        }
      }
    )
  }

  def msg(message: String) = feedback -> message
}
