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
object Accounts extends HtmlController with AccountController with Log {
  val invalidCredentialsMessage = "Invalid credentials."
  val defaultCredentialsMessage = s"Welcome! The default credentials of ${userManager.defaultUser} / ${userManager.defaultPass} have not been changed. " +
    s"Consider changing the password under the Manage tab once you have logged in."
  val passwordChangedMessage = "Password successfully changed."
  val logoutMessage = "You have now logged out."

  def account = PimpAction(implicit req =>
    Ok(html.account(req.user, changePasswordForm))
  )

  val rememberMeLoginForm = Form(tuple(
    userFormKey -> nonEmptyText,
    passFormKey -> nonEmptyText,
    rememberMeKey -> boolean // the checkbox HTML element must have the property 'value="true"'
  ) verifying(invalidCredentialsMessage, _ match {
    case (username, password, _) => validateCredentials(username, password)
  }))

  def login = Action(implicit request => {
    val motd = Option(defaultCredentialsMessage).filter(_ => userManager.isDefaultCredentials)
    Ok(html.login(rememberMeLoginForm, motd))
  })

  def logout = AuthAction(implicit request => {
    // TODO remove the cookie token series, otherwise it will just remain in storage, unused
    Redirect(routes.Accounts.login()).withNewSession.discardingCookies(RememberMe.discardingCookie).flashing(
      FEEDBACK -> logoutMessage
    )
  })

  def formAuthenticate = Action(implicit request => {
    val remoteAddress = request.remoteAddress
    rememberMeLoginForm.bindFromRequest.fold(
      formWithErrors => {
        val user = formWithErrors.data.getOrElse(userFormKey, "")
        log warn s"Authentication failed for user: $user from: $remoteAddress"
        BadRequest(html.login(formWithErrors))
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

  def changePassword = PimpAction(implicit request => {
    val user = request.user
    changePasswordForm.bindFromRequest.fold(
      errors => {
        //        log info "" + errors.globalError + ", " + errors.errors
        BadRequest(html.account(user, errors))
      },
      success => {
        val (_, newPass, _) = success
        userManager updatePassword(user, newPass)
        log info s"Password changed for user: $user from: ${request.remoteAddress}"
        val message = FEEDBACK -> passwordChangedMessage
        Redirect(routes.Accounts.account()).flashing(message)
      }
    )
  })

  override protected def logUnauthorized(implicit request: RequestHeader): Unit = super.logUnauthorized
}
