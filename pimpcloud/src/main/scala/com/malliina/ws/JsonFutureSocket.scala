package com.malliina.ws

import cats.effect.Async
import cats.implicits.toFunctorOps
import com.malliina.musicpimp.cloud.{BodyAndId, UuidFutureMessaging}
import com.malliina.musicpimp.models.CloudID
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.ws.JsonFutureSocket.SuccessKey
import io.circe.{Decoder, Encoder, Json}

import java.util.UUID
import scala.concurrent.duration.{DurationInt, FiniteDuration}

object JsonFutureSocket:
  val RequestId = "request"
  val SuccessKey = "success"

  def tryParseUUID(id: String): Option[UUID] =
    try Option(UUID.fromString(id))
    catch
      case _: IllegalArgumentException =>
        None

/** Emulates an HTTP client using a WebSocket channel. Supports timeouts.
  *
  * Protocol: Responses must be tagged with the same request ID we add to sent messages, so that we
  * can pair requests with responses.
  */
abstract class JsonFutureSocket[F[_]: Async](val id: CloudID) extends UuidFutureMessaging[F]:
  val timeout: FiniteDuration = 20.seconds

  /** We expect responses to contain `RequestId` and `Body` keys, which we parse. The request is
    * completed with the JSON object in `BODY`.
    *
    * @param response
    *   a JSON response from server
    * @return
    *   a response, parsed
    */
  override def extract(response: Json): Option[BodyAndId] =
    response.as[BodyAndId].toOption

  override def isSuccess(response: Json): Boolean =
    response.hcursor.downField(SuccessKey).as[Boolean].forall(_ == true)

  /** Sends `body` as JSON and deserializes the response to `U`.
    *
    * @param data
    *   request
    * @tparam W
    *   type of request payload
    * @tparam T
    *   type of response
    * @return
    *   the response
    */
  def proxyValidated[W: Encoder, T: Decoder](data: PhoneRequest[W]): F[Decoder.Result[T]] =
    defaultProxy(data).map(_.as[T])

  /** @param data
    *   payload
    * @return
    *   response
    */
  def defaultProxy[W: Encoder](data: PhoneRequest[W]): F[Json] =
    request(data, timeout)
