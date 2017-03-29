package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.play.auth.{Authenticator, UserAuthenticator}
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import com.malliina.play.http.AuthedRequest

import scala.concurrent.ExecutionContext

class CloudAuth(auth: Authenticator[AuthedRequest], mat: Materializer)
  extends BaseSecurity(CloudAuth.redirecting(auth), mat)

object CloudAuth {
  def session(mat: Materializer): CloudAuth = new CloudAuth(sessionAuth(mat.executionContext), mat)

  def sessionAuth(ec: ExecutionContext) =
    UserAuthenticator.session().transform((req, user) => Right(AuthedRequest(user, req)))(ec)

  def redirecting(auth: Authenticator[AuthedRequest]): AuthBundle[AuthedRequest] =
    PimpAuths.redirecting(routes.Web.login(), auth)
}
