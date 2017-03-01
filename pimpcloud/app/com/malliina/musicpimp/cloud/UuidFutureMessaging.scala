package com.malliina.musicpimp.cloud

import java.util.UUID

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.Observables
import com.malliina.musicpimp.cloud.UuidFutureMessaging.log
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.play.models.Username
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.Duration
import scala.concurrent.{Future, Promise}

trait UuidFutureMessaging extends FutureMessaging[JsValue] {
  val ongoing = TrieMap.empty[UUID, Promise[JsValue]]

  def extract(response: JsValue): Option[BodyAndId]

  def isSuccess(response: JsValue): Boolean = true

  def request(cmd: String, body: JsValue, user: Username, timeout: Duration): Future[JsValue] =
    request(PhoneRequest(cmd, user, body), timeout)

  def request[W: Writes](req: PhoneRequest[W], timeout: Duration): Future[JsValue] = {
    // generates UUID for this request-response pair
    val uuid = UUID.randomUUID()
    val responsePromise = Promise[JsValue]()
    ongoing += (uuid -> responsePromise)
    // sends the payload, including a request ID
    val payload = Json.toJson(UserRequest(req, uuid))
    send(payload)
    val task = responsePromise.future
    // fails promise after timeout
    if (!responsePromise.isCompleted) {
      Observables.after(timeout) {
        ongoing -= uuid
        if (!responsePromise.isCompleted) {
          val message = s"Request: $uuid timed out after: $timeout."
          val failed = responsePromise tryFailure new concurrent.TimeoutException(message)
          if (failed) {
            log warn message
          }
        }
      }
    }
    task
  }

  def complete(response: JsValue): Boolean =
    extract(response) exists { pair =>
      val uuid = pair.uuid
      val body = pair.body
      if (isSuccess(response)) succeed(uuid, body)
      else fail(uuid, body)
    }

  /** Completes the ongoing [[Promise]] identified by `requestID` with `responseBody`.
    *
    * @param requestID    the request ID
    * @param responseBody the payload of the response, that is, the 'body' JSON value
    * @return true if an ongoing request with ID `requestID` existed, false otherwise
    */
  def succeed(requestID: UUID, responseBody: JsValue): Boolean =
    baseComplete(requestID)(_.trySuccess(responseBody))

  /** Fails the ongoing [[Promise]] identified by `requestID` with a [[RequestFailure]] containing `responseBody`.
    *
    * @param requestID    request ID
    * @param responseBody body of failed response
    * @return true if an ongoing request with ID `requestID` existed, false otherwise
    */
  def fail(requestID: UUID, responseBody: JsValue) =
    failExceptionally(requestID, new RequestFailure(responseBody))

  def failExceptionally(requestID: UUID, t: Throwable) =
    baseComplete(requestID)(_.tryFailure(t))

  private def baseComplete(request: UUID)(f: Promise[JsValue] => Unit) = {
    (ongoing get request).exists { promise =>
      f(promise)
      ongoing -= request
      true
    }
  }

  class RequestFailure(val response: JsValue) extends Exception

  case class BodyAndId(body: JsValue, uuid: UUID)

}

object UuidFutureMessaging {
  private val log = Logger(getClass)
}
