package controllers

import com.malliina.musicpimp.json.JsonFormatVersions
import controllers.PimpContentController.log
import play.api.Logger
import play.api.http.{MimeTypes, Writeable}
import play.api.libs.json.{JsValue, Json, Writes}
import play.api.mvc.Results.{Accepted, Ok, Status}
import play.api.mvc.{RequestHeader, Result, Results}

import scala.concurrent.Future

/** Methods that choose the correct response to provide to clients
  * based on what they accept (HTML/JSON/which JSON version).
  */
trait PimpContentController {

  import com.malliina.musicpimp.json.JsonFormatVersions._

  def pimpResponse(request: RequestHeader)(html: => Result, json17: => JsValue, latest: => JsValue): Result = {
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) =>
        html
      case Some(JSONv17) =>
        Ok(json17)
      case Some(JSONv18) =>
        Ok(latest)
      //      case Some(JSONv24) => Ok(latest)
      case Some(other) =>
        val msg = s"Unknown response format: '$other'."
        log.warn(msg)
        notAcceptable(msg)
      case None =>
        val msg = "No requested response format, unacceptable. Please provide a value in the 'Accept' header."
        log.warn(msg)
        notAcceptable(msg)
    }
  }

  def notAcceptable(msg: String) = PimpContentController.notAcceptable(msg)

  def respond[C: Writeable, T: Writes](request: RequestHeader)(html: => C, json: => T, status: Status = Ok): Result =
    pimpResult(request)(status(html), status(Json.toJson(json)))

  /**
    * @return the equivalent of "Unit" in JSON and HTML
    */
  def AckResponse(request: RequestHeader) =
    pimpResult(request)(html = Accepted, json = Accepted)

  def pimpResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    PimpContentController.pimpResult(request)(html, json)

  def pimpResult2(request: RequestHeader)(html: => Future[Result], json: => Result): Future[Result] =
    PimpContentController.pimpResult2(request)(html, json)
}

object PimpContentController {
  private val log = Logger(getClass)
  val JsonKey = "json"

  def notAcceptableGeneric = notAcceptable("Please use the 'Accept' header.")

  def notAcceptable(msg: String) = Errors.withStatus(Results.NotAcceptable, msg)

  // TODO dry

  def pimpResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains JsonKey => json
      case _ => notAcceptableGeneric
    }

  def pimpResult2(request: RequestHeader)(html: => Future[Result], json: => Result): Future[Result] =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains JsonKey => fut(json)
      case _ => fut(notAcceptableGeneric)
    }
}

object PimpRequest {

  import com.malliina.musicpimp.json.JsonFormatVersions._

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

  /** The desired format for clients compatible with API version 17 is
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
