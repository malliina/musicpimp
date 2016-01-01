package controllers

import com.malliina.play.Authenticator
import play.api.mvc.{EssentialAction, RequestHeader}

/**
 *
 * @author mle
 */
class HtmlController(auth: Authenticator) extends Secured(auth) {
  protected def navigate(page: => play.twirl.api.Html): EssentialAction = navigate(_ => page)

  protected def navigate(f: RequestHeader => play.twirl.api.Html): EssentialAction =
    PimpAction(implicit req => Ok(f(req)))
}
