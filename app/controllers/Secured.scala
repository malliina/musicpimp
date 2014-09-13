package controllers

import com.mle.util.Log
import play.api.mvc._

/**
 *
 * @author mle
 */
trait Secured extends SecureBase with PimpContentController with Log {
  val INTENDED_URI = "intended_uri"

  protected def logUnauthorized(implicit request: RequestHeader) {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    log info s"Intended: ${request.uri}"
    pimpResult(
      html = Redirect(routes.Website.login()).withSession(INTENDED_URI -> request.uri),
      json = Unauthorized
    )
  }
}