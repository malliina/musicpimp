package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.http.OkClient
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.play.auth.{AuthConf, AuthFailure, Authenticator, BasicAuthHandler, CodeValidationConf, StandardCodeValidator, UserAuthenticator}
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Email
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc._

class ProdAuth(ctrl: OAuthCtrl) extends PimpAuth {
  override def logged(action: EssentialAction) =
    ctrl.logged(action)

  override def authenticate(request: RequestHeader) =
    ctrl.authenticate(request)

  override def authAction(f: AuthedRequest => Result) =
    ctrl.authAction(f)
}

trait PimpAuth extends Authenticator[AuthedRequest] {
  def logged(action: EssentialAction): EssentialAction

  def authAction(f: AuthedRequest => Result): EssentialAction

  def navigate[C: Writeable](f: AuthedRequest => C): EssentialAction =
    authAction(req => Ok(f(req)))
}

class OAuthCtrl(val oauth: AdminOAuth, mat: Materializer)
  extends BaseSecurity(oauth.actions, OAuthCtrl.bundle(oauth), mat)

object OAuthCtrl {
  def bundle(oauth: AdminOAuth) = new AuthBundle[AuthedRequest] {
    override def authenticator: Authenticator[AuthedRequest] =
      UserAuthenticator.session(oauth.sessionKey)
        .transform((req, user) => Right(AuthedRequest(user, req)))

    override def onUnauthorized(failure: AuthFailure): Result =
      Results.Redirect(routes.AdminOAuth.googleStart())
  }
}

class AdminOAuth(val actions: ActionBuilder[Request, AnyContent], creds: GoogleOAuthCredentials) {
  val sessionKey = "username"
  val authorizedEmail = Email("malliina123@gmail.com")
  val handler = BasicAuthHandler(routes.Logs.index()).filter(_ == authorizedEmail)
  val conf = AuthConf(creds.clientId, creds.clientSecret)
  val validator = StandardCodeValidator(CodeValidationConf.google(routes.AdminOAuth.googleCallback(), handler, conf, OkClient.default))

  def googleStart = actions.async { req => validator.start(req) }

  def googleCallback = actions.async { req => validator.validateCallback(req) }
}
