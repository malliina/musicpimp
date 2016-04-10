package controllers

import akka.stream.Materializer
import com.malliina.play.Authenticator
import controllers.HtmlController.log
import play.api.Logger
import play.api.mvc.{EssentialAction, RequestHeader}
import play.twirl.api.Html

class HtmlController(auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {
  protected def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  protected def navigate(f: RequestHeader => Html): EssentialAction =
    PimpAction(req => {
//      log info s"Serving ${req.path}"
      Ok(f(req))
    })
}

object HtmlController {
  private val log = Logger(getClass)
}
