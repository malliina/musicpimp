package controllers

import views.html
import play.api.mvc._
import com.mle.util.{Log, FileUtilities}
import scala.Some
import com.mle.musicpimp.util.FileUtil
import com.mle.play.controllers.AccountController

/**
 * @author Michael
 */
trait PimpAccountController extends HtmlController with AccountController with Log {

  import PimpAccountController._

  def account = PimpAction(implicit req =>
    Ok(html.account(req.user, changePasswordForm))
  )

  def login = Action(implicit request => {
    val motd =
      if (validateCredentials(defaultUser, defaultPass)) {
        Some(s"Welcome! The default credentials of $defaultUser / $defaultPass have not been changed. " +
          s"Consider changing the password under the Manage tab once you have logged in.")
      } else {
        None
      }
    Ok(html.login(loginForm, motd))
  })

  def logout = AuthAction(implicit request => {
    Redirect(routes.Website.login()).withNewSession.flashing(
      "feedback" -> "You have now logged out."
    )
  })

  def formAuthenticate = Action(implicit request => {
    val remoteAddress = request.remoteAddress
    loginForm.bindFromRequest.fold(
      formWithErrors => {
        val user = formWithErrors.data.get("username").getOrElse("")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        BadRequest(html.login(formWithErrors))
      },
      credentials => {
        val user = credentials._1
        log info s"Authentication succeeded for user: $user from: $remoteAddress"
        Redirect(routes.Website.rootLibrary()).withSession(Security.username -> credentials._1)
      }
    )
  })

  def changePassword = PimpAction(implicit request => {
    val user = request.user
    changePasswordForm.bindFromRequest.fold(
      errors => {
        log info "" + errors.globalError + ", " + errors.errors
        BadRequest(html.account(user, errors))
      },
      success => {
        val (_, newPass, _) = success
        setPassword(user, newPass)
        log info s"Password changed for user: $user from: ${request.remoteAddress}"
        val message = "feedback" -> "Password successfully changed."
        Redirect(routes.Website.account()).flashing(message)
      }
    )
  })


  private def setPassword(username: String, password: String) {
    FileUtilities.writerTo(passFile)(passWriter => {
      passWriter write hash(username, password)
    })
    FileUtil trySetOwnerOnlyPermissions passFile
  }
}

object PimpAccountController {
  val passFile = FileUtilities pathTo "credentials.txt"
  val defaultUser = "admin"
  val defaultPass = "test"
}
