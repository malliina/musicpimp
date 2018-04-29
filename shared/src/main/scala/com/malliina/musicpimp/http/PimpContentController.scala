package com.malliina.musicpimp.http

import com.malliina.musicpimp.http.PimpContentController.log
import com.malliina.musicpimp.json.JsonFormatVersions
import com.malliina.musicpimp.models.Errors
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

  import JsonFormatVersions._

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

  def pimpResponseOkJson(request: RequestHeader)(html: => Result, json: => JsValue): Result =
    pimpResult(request)(html, Ok(json))

  /**
    * @return the equivalent of "Unit" in JSON and HTML
    */
  def AckResponse(request: RequestHeader) =
    pimpResult(request)(html = Accepted, json = Accepted)

  def pimpResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    PimpContentController.pimpResult(request)(html, json)

  def pimpResult2(request: RequestHeader)(html: => Future[Result], json: => Result): Future[Result] =
    PimpContentController.pimpResultUneven(request)(html, json)
}

object PimpContentController {
  private val log = Logger(getClass)
  val JsonKey = "json"

  object default extends PimpContentController

  def notAcceptableGeneric = notAcceptable("Please use the 'Accept' header.")

  def notAcceptable(msg: String) = Errors.withStatus(Results.NotAcceptable, msg)

  def pimpResult(request: RequestHeader)(html: => Result, json: => Result): Result =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains JsonKey => json
      case _ => notAcceptableGeneric
    }

  def pimpResultUneven(request: RequestHeader)(html: => Future[Result], json: => Result): Future[Result] =
    pimpResultAsync(request)(html = html, json = fut(json))

  def pimpResultAsync(request: RequestHeader)(html: => Future[Result], json: => Future[Result]): Future[Result] =
    PimpRequest.requestedResponseFormat(request) match {
      case Some(MimeTypes.HTML) => html
      case Some(format) if format contains JsonKey => json
      case _ => fut(notAcceptableGeneric)
    }

  def fut[T](t: T) = Future.successful(t)
}
