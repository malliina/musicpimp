package controllers

import play.api.mvc.{RequestHeader, EssentialAction}
import play.api.templates.Html

/**
 *
 * @author mle
 */
trait HtmlController extends Secured {
  protected def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  protected def navigate(f: RequestHeader => Html): EssentialAction =
    PimpAction(implicit req => Ok(f(req)))
}
