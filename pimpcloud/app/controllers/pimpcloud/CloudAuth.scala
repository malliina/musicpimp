package controllers.pimpcloud

import org.apache.pekko.stream.Materializer
import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.play.auth.{Authenticator, UserAuthenticator}
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import com.malliina.play.http.AuthedRequest
import play.api.mvc.{ActionBuilder, AnyContent, Request}

import scala.concurrent.ExecutionContext

class CloudAuth(
  actions: ActionBuilder[Request, AnyContent],
  auth: Authenticator[AuthedRequest],
  mat: Materializer
) extends BaseSecurity(actions, CloudAuth.redirecting(auth), mat)

object CloudAuth {
  def session(actions: ActionBuilder[Request, AnyContent], mat: Materializer): CloudAuth =
    new CloudAuth(actions, sessionAuth(mat.executionContext), mat)

  def sessionAuth(ec: ExecutionContext) =
    UserAuthenticator.session().transform((req, user) => Right(AuthedRequest(user, req)))(ec)

  def redirecting(auth: Authenticator[AuthedRequest]): AuthBundle[AuthedRequest] =
    PimpAuths.redirecting(routes.Web.login, auth)
}
