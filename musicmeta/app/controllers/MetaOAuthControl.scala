package controllers

import com.malliina.http.io.HttpClientIO
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.play.auth.{AuthHandler, GoogleCodeValidator, OAuthConf}
import com.malliina.values.Email
import com.malliina.web.{AuthConf, AuthError, ClientId, ClientSecret}
import play.api.libs.json.Json
import play.api.mvc.Results.{Redirect, Unauthorized}
import play.api.mvc._

class MetaOAuthControl(
  val actions: ActionBuilder[Request, AnyContent],
  creds: GoogleOAuthCredentials
) {
  val http = HttpClientIO()
  val handler: AuthHandler = new AuthHandler {
    override def onAuthenticated(email: Email, req: RequestHeader): Result =
      if (email == Email("malliina123@gmail.com"))
        Redirect(routes.MetaOAuth.logs).withSession("username" -> email.email)
      else
        ejectWith(s"Not authorized: '$email'.")

    override def onUnauthorized(error: AuthError, req: RequestHeader): Result =
      Unauthorized(Json.obj("message" -> "Authentication failed."))

    def ejectWith(message: String) =
      Redirect(routes.MetaOAuth.eject).flashing("message" -> message)
  }
  val authConf = OAuthConf(
    routes.MetaOAuthControl.googleCallback,
    handler,
    AuthConf(ClientId(creds.clientId), ClientSecret(creds.clientSecret)),
    http
  )
  val validator = GoogleCodeValidator(authConf)

  def googleStart = actions.async { req => validator.start(req).unsafeToFuture() }

  def googleCallback = actions.async { req => validator.validateCallback(req).unsafeToFuture() }
}
