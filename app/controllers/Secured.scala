package controllers

import com.mle.util.Log
import play.api.mvc._

/**
 *
 * @author mle
 */
trait Secured extends SecureBase with PimpContentController with Log {
  protected def logUnauthorized(implicit request: RequestHeader) {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    pimpResult(
      html = Redirect(routes.Website.login()),
      json = Unauthorized
    )
  }
}