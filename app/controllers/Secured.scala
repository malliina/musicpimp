package controllers

import com.mle.util.Log
import play.api.mvc._

/**
 *
 * @author mle
 */
trait Secured extends SecureBase with PimpContentController with Log {
  protected def logUnauthorized(implicit request: RequestHeader): Unit = {
    log warn "Unauthorized request: " + request.path + " from: " + request.remoteAddress
  }

  protected override def onUnauthorized(implicit request: RequestHeader): Result = {
    logUnauthorized(request)
    log debug s"Intended: ${request.uri}"
    pimpResult(
      html = Redirect(routes.Accounts.login()).withSession(Accounts.INTENDED_URI -> request.uri),
      json = Unauthorized
    )
  }
}