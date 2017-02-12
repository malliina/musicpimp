package controllers

import akka.stream.Materializer
import com.malliina.oauth.GoogleOAuthCredentials
import com.malliina.play.controllers.{OAuthControl, OAuthSecured}
import com.malliina.play.http.{AuthedRequest, FullRequest}
import play.api.http.Writeable
import play.api.mvc.Results.Ok
import play.api.mvc.{Call, EssentialAction, RequestHeader, Result}

import scala.concurrent.Future

class ProdAuth(ctrl: OAuthCtrl) extends PimpAuth {
  override def logged(action: EssentialAction) = ctrl.logged(action)

  override def authenticate(request: RequestHeader) = ctrl.authenticate(request)

  override def authAction(f: FullRequest => Result) = ctrl.authAction(f)
}

trait PimpAuth {
  def logged(action: EssentialAction): EssentialAction

  def authenticate(request: RequestHeader): Future[Option[AuthedRequest]]

  def authAction(f: FullRequest => Result): EssentialAction

  def navigate[C: Writeable](f: RequestHeader => C): EssentialAction =
    authAction(req => Ok(f(req)))
}

class OAuthCtrl(val oauth: AdminOAuth) extends OAuthSecured(oauth, oauth.mat)

class AdminOAuth(creds: GoogleOAuthCredentials, val mat: Materializer) extends OAuthControl(creds, mat) {
  override val sessionUserKey: String = "email"

  override def isAuthorized(email: String): Boolean = email == "malliina123@gmail.com"

  override def startOAuth: Call = routes.AdminOAuth.initiate()

  override def oAuthRedir: Call = routes.AdminOAuth.redirResponse()

  override def onOAuthSuccess: Call = routes.Logs.index()

  override def ejectCall: Call = routes.AdminAuth.eject()
}
