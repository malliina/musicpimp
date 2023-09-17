package controllers.musicpimp

import com.malliina.musicpimp.auth.PimpAuths
import com.malliina.play.auth.Authenticator
import com.malliina.play.controllers.AuthBundle
import com.malliina.play.http.{AuthedRequest, Proxies}
import play.api.Logger
import play.api.mvc._

class Secured(auth: AuthDeps) extends SecureBase(auth)

object Secured {
  private val log = Logger(getClass)

  def logUnauthorized(request: RequestHeader): Unit = {
    val remoteAddress = Proxies.realAddress(request)
    log.warn(s"Unauthorized request '${request.path}' from '$remoteAddress'.")
  }

  def redirecting(auth: Authenticator[AuthedRequest]): AuthBundle[AuthedRequest] =
    PimpAuths.redirecting(routes.Accounts.loginPage, auth)
}
