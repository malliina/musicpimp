package controllers

import controllers.PimpContentController.log
import play.api.Logger
import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.mvc.Results.{Accepted, NotAcceptable, Ok, Status}
import play.api.mvc.{RequestHeader, Result}
import play.twirl.api.Html

import scala.concurrent.Future

/** Methods that choose the correct response to provide to clients
  * based on what they accept (HTML/JSON/which JSON version).
  */
trait PimpContentController {

  import com.malliina.musicpimp.json.JsonFormatVersions._

  def pimpResponse(request: RequestHeader)(html: => Result, json17: => JsValue, latest: => JsValue): Result = {
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(JSONv17) => Ok(json17)
      case Some(JSONv18) => Ok(latest)
      //      case Some(JSONv24) => Ok(latest)
      case Some(other) =>
        log.warn(s"Client requests unknown response format: $other")
        NotAcceptable
      case None =>
        log.warn("No requested response format, unacceptable.")
        NotAcceptable
    }
  }

  def pimpResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => json
      case _ => NotAcceptable
    }

  // TODO dry
  def pimpResultAsync(request: RequestHeader)(html: => Future[Result], json: => Future[Result]): Future[Result] =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => json
      case _ => Future.successful(NotAcceptable)
    }

  def pimpResponse(request: RequestHeader)(html: => Result, json: => JsValue): Result =
    pimpResult(request)(html, Ok(json))

  def respond(request: RequestHeader)(html: => Html, json: => JsValue, status: Status = Ok): Result =
    pimpResult(request)(status(html), status(json))

  /**
    *
    * @return the equivalent of "Unit" in JSON and HTML
    */
  def ackResponse(request: RequestHeader) =
    pimpResult(request)(html = Accepted, json = Accepted)
}

object PimpContentController extends PimpContentController {
  private val log = Logger(getClass)
}
