package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.play.auth.{AuthFailure, Authenticator, UserAuthenticator}
import com.malliina.play.controllers.{AuthBundle, BaseSecurity, OAuthControl}
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Email
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc._

import scala.concurrent.ExecutionContext

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

class OAuthCtrl(val oauth: AdminOAuth)
  extends BaseSecurity(oauth.actions, OAuthCtrl.bundle(oauth, oauth.ec), oauth.mat)

object OAuthCtrl {
  def bundle(oauth: OAuthControl, ec: ExecutionContext) = new AuthBundle[AuthedRequest] {
    override def authenticator: Authenticator[AuthedRequest] =
      UserAuthenticator.session(oauth.sessionUserKey)
        .transform((req, user) => Right(AuthedRequest(user, req)))(ec)

    override def onUnauthorized(failure: AuthFailure): Result =
      Results.Redirect(oauth.startOAuth)
  }
}

class AdminOAuth(actions: ActionBuilder[Request, AnyContent], creds: GoogleOAuthCredentials, val mat: Materializer)
  extends OAuthControl(actions, creds) {

  override def isAuthorized(email: Email): Boolean = email == Email("malliina123@gmail.com")

  override def startOAuth: Call = routes.AdminOAuth.initiate()

  override def oAuthRedir: Call = routes.AdminOAuth.redirResponse()

  override def onOAuthSuccess: Call = routes.Logs.index()

  override def ejectCall: Call = routes.AdminAuth.eject()
}
