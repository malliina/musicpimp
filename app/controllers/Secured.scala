package controllers

import akka.stream.Materializer
import com.malliina.play.Authenticator
import play.api.Logger
import play.api.mvc._

class Secured(auth: Authenticator, mat: Materializer) extends SecureBase(auth, mat) {
  protected def logUnauthorized(implicit request: RequestHeader): Unit = {
    Secured.log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    Secured.log debug s"Intended: ${request.uri}"
    pimpResult(
      html = Redirect(routes.Accounts.login()).withSession(Accounts.IntendedUri -> request.uri),
      json = Unauthorized
    )
  }
}

object Secured {
  private val log = Logger(getClass)
}
