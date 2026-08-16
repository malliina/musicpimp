package com.malliina.musicpimp.cloud

import cats.effect.{Async, Deferred}
import cats.implicits.*
import com.malliina.musicpimp.cloud.UuidFutureMessaging.log
import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.util.AppLogger
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration

object UuidFutureMessaging:
  private val log = AppLogger(getClass)

abstract class UuidFutureMessaging[F[_]: Async] extends FutureMessaging[F, Json]:
  val F = Async[F]
  val ongoing = TrieMap.empty[RequestID, Deferred[F, Either[Throwable, Json]]]

  def extract(response: Json): Option[BodyAndId]

  def isSuccess(response: Json): Boolean = true

  def request[W: Encoder](req: PhoneRequest[W], timeout: FiniteDuration): F[Json] =
    // generates UUID for this request-response pair
    val request = RequestID.random()
    Deferred[F, Either[Throwable, Json]].flatMap: deferred =>
      ongoing += (request -> deferred)
      // sends the payload, including a request ID
      val payload = UserRequest(req, request).asJson
      send(payload).flatMap: _ =>
        val onTimeout = F.delay[Either[Throwable, Json]]:
          ongoing -= request
          val message = s"Request: $request timed out after: $timeout."
          log.warn(message)
          Left(new concurrent.TimeoutException(message))

        F.timeoutTo(deferred.get, timeout, onTimeout)
          .flatMap: res =>
            res.fold(t => F.raiseError(t), ok => F.pure(ok))

  def complete(response: Json): F[Boolean] =
    extract(response).fold(F.pure(false)): pair =>
      val uuid = pair.request
      val body = pair.body
      if isSuccess(response) then succeed(uuid, body)
      else fail(uuid, body)

  /** Completes the ongoing [[Deferred]] identified by `requestID` with `responseBody`.
    *
    * @param requestID
    *   the request ID
    * @param responseBody
    *   the payload of the response, that is, the 'body' JSON value
    * @return
    *   true if an ongoing request with ID `requestID` existed, false otherwise
    */
  private def succeed(requestID: RequestID, responseBody: Json): F[Boolean] =
    baseComplete(requestID)(_.complete(Right(responseBody)))

  /** Fails the ongoing [[Deferred]] identified by `requestID` with a [[RequestFailure]] containing
    * `responseBody`.
    *
    * @param requestID
    *   request ID
    * @param responseBody
    *   body of failed response
    * @return
    *   true if an ongoing request with ID `requestID` existed, false otherwise
    */
  private def fail(requestID: RequestID, responseBody: Json): F[Boolean] =
    log.error(s"Failing request '$requestID', response '$responseBody'.")
    baseComplete(requestID)(_.complete(Left(new RequestFailure(responseBody))))

  private def baseComplete(
    request: RequestID
  )(f: Deferred[F, Either[Throwable, Json]] => F[Boolean]): F[Boolean] =
    ongoing
      .get(request)
      .fold(F.pure(false)): promise =>
        f(promise).flatTap(_ => F.delay(ongoing -= request)).as(true)
