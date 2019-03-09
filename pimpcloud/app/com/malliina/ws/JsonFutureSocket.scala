package com.malliina.ws

import java.util.UUID

import akka.actor.Scheduler
import com.malliina.musicpimp.cloud.{BodyAndId, UuidFutureMessaging}
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.ws.JsonFutureSocket.SuccessKey
import com.malliina.concurrent.Execution.cached
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object JsonFutureSocket {
  val RequestId = "request"
  val SuccessKey = "success"

  def tryParseUUID(id: String): Option[UUID] =
    try {
      Option(UUID.fromString(id))
    } catch {
      case _: IllegalArgumentException =>
        None
    }
}

/** Emulates an HTTP client using a WebSocket channel. Supports timeouts.
  *
  * Protocol: Responses must be tagged with the same request ID we add to sent messages, so that we can
  * pair requests with responses.
  */
abstract class JsonFutureSocket(val id: CloudID, scheduler: Scheduler)
    extends UuidFutureMessaging(scheduler)(cached) {

  val timeout = 20.seconds

  /** We expect responses to contain `RequestId` and `Body` keys, which we parse. The request is completed with the
    * JSON object in `BODY`.
    *
    * @param response a JSON response from server
    * @return a response, parsed
    */
  override def extract(response: JsValue): Option[BodyAndId] =
    response.asOpt[BodyAndId]

  override def isSuccess(response: JsValue): Boolean =
    (response \ SuccessKey).validate[Boolean].filter(_ == false).isError

  /** Sends `body` as JSON and deserializes the response to `U`.
    *
    * @param data request
    * @tparam W type of request payload
    * @tparam T type of response
    * @return the response
    */
  def proxyValidated[W: Writes, T: Reads](data: PhoneRequest[W]): Future[JsResult[T]] =
    defaultProxy(data).map(_.validate[T])

  /**
    * @param data payload
    * @return response
    */
  def defaultProxy[W: Writes](data: PhoneRequest[W]): Future[JsValue] =
    request(data, timeout)
}
