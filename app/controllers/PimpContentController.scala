package controllers

import com.mle.musicpimp.json.JsonFormatVersions
import com.mle.util.Log
import play.api.http.MimeTypes
import play.api.libs.json.JsValue
import play.api.mvc.{Controller, RequestHeader, Result}

import scala.concurrent.Future

/**
 * Methods that choose the correct response to provide to clients
 * based on what they accept (HTML/JSON/which JSON version).
 *
 * @author mle
 */
trait PimpContentController extends Controller with Log {

  import com.mle.musicpimp.json.JsonFormatVersions._

  def pimpResponse(html: => Result, json17: => JsValue, latest: => JsValue)(implicit request: RequestHeader): Result = {
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

  def pimpResult(html: => Result, json: => Result)(implicit request: RequestHeader): Result =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => json
      case _ => NotAcceptable
    }

  // TODO dry
  def pimpResult(html: => Future[Result], json: => Result)(implicit request: RequestHeader): Future[Result] =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains "json" => Future.successful(json)
      case _ => Future.successful(NotAcceptable)
    }

  def pimpResponse(html: => Result, json: => JsValue)(implicit request: RequestHeader): Result =
    pimpResult(html, Ok(json))

  def respond(html: => play.twirl.api.Html, json: => JsValue, status: Status = Ok)(implicit request: RequestHeader): Result =
    pimpResult(status(html), status(json))

  /**
   *
   * @return the equivalent of "Unit" in JSON and HTML
   */
  def AckResponse(implicit request: RequestHeader) =
    pimpResult(html = Accepted, json = Accepted)
}

object PimpContentController extends PimpContentController

object PimpRequest extends Log {

  import com.mle.musicpimp.json.JsonFormatVersions._

  /**
   * @param request the request
   * @return the desired format of the response to `request`.
   */
  def requestedResponseFormat(request: RequestHeader): Option[String] = {
    //    log.info(s"Headers: ${PlayUtils.headersString(request)}")
    if (request.getQueryString("f").map(_ == "json").isDefined) Some(latest)
    else if (request accepts MimeTypes.HTML) Some(MimeTypes.HTML)
    else if (request accepts anyJson) Some(latest)
    else if (request accepts JSONv17) Some(JSONv17)
    else if ((request accepts JSONv18) || (request accepts MimeTypes.JSON)) Some(JSONv18)
    //    else if ((request accepts JSONv24) || (request accepts MimeTypes.JSON)) Some(JSONv24)
    else None
  }

  /**
   * The desired format for clients compatible with API version 17 is
   * incorrectly determined to be HTML, because those clients do not
   * specify an Accept header in their WebSocket requests thus the server
   * thinks they are browsers by default. However, the WebSocket API does
   * not support HTML, only JSON, so we can safely assume they are JSON
   * clients and since clients newer than version 17 must use the Accept
   * header, we can conclude that they are API version 17 JSON clients.
   *
   * Therefore we can filter out HTML formats as below and default to API
   * version 17 unless the client explicitly requests otherwise.
   *
   * This is a workaround to ensure API compatibility during a transition
   * period from a non-versioned API to a versioned one. Once the transition
   * is complete, we should default to the latest API version.
   */
  def apiVersion(header: RequestHeader) =
    PimpRequest.requestedResponseFormat(header)
      .filter(_ != MimeTypes.HTML)
      .getOrElse(JsonFormatVersions.JSONv17)
}
