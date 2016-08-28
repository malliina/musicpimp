package controllers

import akka.stream.Materializer
import com.malliina.play.Authenticator
import controllers.Secured.log
import play.api.Logger
import play.api.mvc._

class Secured(auth: Authenticator, mat: Materializer)
  extends SecureBase(auth, mat) {

  protected def logUnauthorized(request: RequestHeader): Unit = {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(request: RequestHeader): Result = {
    logUnauthorized(request)
    log debug s"Intended: ${request.uri}"
    pimpResult(request)(
      html = Redirect(routes.Accounts.login()).withSession(Accounts.IntendedUri -> request.uri),
      json = Unauthorized
    )
  }
}

object Secured {
  private val log = Logger(getClass)
}
