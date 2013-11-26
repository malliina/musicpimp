package controllers

import play.api.mvc._
import com.mle.util.Log

/**
 *
 * @author mle
 */
trait Secured extends SecureBase with PimpContentController with Log {
  protected def logUnauthorized(implicit request: RequestHeader) {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): SimpleResult = {
    logUnauthorized(request)
    pimpResult(
      html = Redirect(routes.Website.login()),
      json = Unauthorized
    )
  }
}