package com.malliina.ws

import java.util.UUID

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.cloud.{BodyAndId, UuidFutureMessaging}
import com.malliina.musicpimp.models.{CloudID, RequestID}
import com.malliina.pimpcloud.json.JsonStrings.Body
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import com.malliina.util.Utils
import com.malliina.ws.JsonFutureSocket.{RequestId, SuccessKey}
import play.api.libs.json._

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

/** Emulates an HTTP client using a WebSocket channel. Supports timeouts.
  *
  * Protocol: Responses must be tagged with the same request ID we add to sent messages, so that we can
  * pair requests with responses.
  */
abstract class JsonFutureSocket(val id: CloudID)
  extends UuidFutureMessaging {

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
    * @param body message payload
    * @tparam T type of request payload
    * @tparam U type of response
    * @return the response
    */
  def proxyT[T: Writes, U: Reads](cmd: String, user: Username, body: T): Future[U] =
    proxyD(PhoneRequest[T](cmd, user, body))

  /** Sends `body` and deserializes the response to type `T`.
    *
    * TODO check success status first, and any potential error
    *
    * @param data payload
    * @tparam T type of response
    * @return a deserialized body, or a failed [[Future]] on failure
    */
  def proxyD[W: Writes, T: Reads](data: PhoneRequest[W]): Future[T] =
    proxyD2[W, T](data).map(_.get)

  def proxyD2[W: Writes, T: Reads](data: PhoneRequest[W]): Future[JsResult[T]] =
    defaultProxy(data).map(_.validate[T])

  /**
    * @param data payload
    * @return response
    */
  def defaultProxy[W: Writes](data: PhoneRequest[W]): Future[JsValue] =
    request(data, timeout)
}

object JsonFutureSocket {
  val RequestId = "request"
  val SuccessKey = "success"

  def tryParseUUID(id: String): Option[UUID] = {
    Utils.opt[UUID, IllegalArgumentException](UUID.fromString(id))
  }
}
