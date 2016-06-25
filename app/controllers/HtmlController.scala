package controllers

import akka.stream.Materializer
import com.malliina.play.Authenticator
import play.api.mvc.{EssentialAction, RequestHeader}
import play.twirl.api.Html

class HtmlController(auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {
  protected def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  protected def navigate(f: RequestHeader => Html): EssentialAction =
    pimpAction(req => Ok(f(req)))
}
