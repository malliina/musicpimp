package controllers

import akka.stream.Materializer
import com.malliina.play.Authenticator
import com.malliina.play.http.CookiedRequest
import com.malliina.play.models.Username
import play.api.http.Writeable
import play.api.mvc.{AnyContent, EssentialAction}
import play.twirl.api.Html

class HtmlController(auth: Authenticator, mat: Materializer) extends Secured(auth, mat) {
  protected def navigate(page: => Html): EssentialAction =
    navigate(_ => page)

  protected def navigate[C: Writeable](f: CookiedRequest[AnyContent, Username] => C): EssentialAction =
    pimpAction(req => Ok(f(req)))
}
