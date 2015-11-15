package controllers

import com.mle.play.Authenticator
import play.api.Logger
import play.api.mvc._

/**
  *
  * @author mle
  */
class Secured(auth: Authenticator) extends SecureBase(auth) {
  protected def logUnauthorized(implicit request: RequestHeader): Unit = {
    Secured.log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    Secured.log debug s"Intended: ${request.uri}"
    pimpResult(
      html = Redirect(routes.Accounts.login()).withSession(Accounts.INTENDED_URI -> request.uri),
      json = Unauthorized
    )
  }
}

object Secured {
  private val log = Logger(getClass)
}
