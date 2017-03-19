package controllers.pimpcloud

import akka.stream.Materializer
import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.{AuthBundle, BaseSecurity}
import com.malliina.play.http.AuthedRequest
import play.api.Logger

class CloudAuth(auth: Authenticator[AuthedRequest], mat: Materializer)
  extends BaseSecurity(CloudAuth.redirecting(auth), mat)

object CloudAuth {
  private val log = Logger(getClass)

  def redirecting(auth: Authenticator[AuthedRequest]): AuthBundle[AuthedRequest] =
    PimpAuths.redirecting(routes.Web.login(), auth)
}
