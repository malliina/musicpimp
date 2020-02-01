package com.malliina.musicpimp.cloud

import akka.actor.Scheduler
import akka.pattern.after
import com.malliina.musicpimp.cloud.UuidFutureMessaging.log
import com.malliina.musicpimp.models.RequestID
import com.malliina.pimpcloud.models.PhoneRequest
import play.api.Logger
import play.api.libs.json.{JsValue, Json, Writes}

import scala.collection.concurrent.TrieMap
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}

object UuidFutureMessaging {
  private val log = Logger(getClass)
}

abstract class UuidFutureMessaging(scheduler: Scheduler)(implicit ec: ExecutionContext)
  extends FutureMessaging[JsValue] {
  val ongoing = TrieMap.empty[RequestID, Promise[JsValue]]

  def extract(response: JsValue): Option[BodyAndId]

  def isSuccess(response: JsValue): Boolean = true

  def request[W: Writes](req: PhoneRequest[W], timeout: FiniteDuration): Future[JsValue] = {
    // generates UUID for this request-response pair
    val request = RequestID.random()
    val responsePromise = Promise[JsValue]()
    ongoing += (request -> responsePromise)
    // sends the payload, including a request ID
    val payload = Json.toJson(UserRequest(req, request))
    send(payload)
    val task = responsePromise.future
    // fails promise after timeout
    if (!responsePromise.isCompleted) {
      after(timeout, scheduler) {
        Future {
          ongoing -= request
          if (!responsePromise.isCompleted) {
            val message = s"Request: $request timed out after: $timeout."
            val failed = responsePromise tryFailure new concurrent.TimeoutException(message)
            if (failed) {
              log warn message
            }
          }
        }
      }
    }
    task
  }

  def complete(response: JsValue): Boolean =
    extract(response).exists { pair =>
      val uuid = pair.request
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
  def succeed(requestID: RequestID, responseBody: JsValue): Boolean =
    baseComplete(requestID)(_.trySuccess(responseBody))

  /** Fails the ongoing [[Promise]] identified by `requestID` with a [[RequestFailure]] containing `responseBody`.
    *
    * @param requestID    request ID
    * @param responseBody body of failed response
    * @return true if an ongoing request with ID `requestID` existed, false otherwise
    */
  def fail(requestID: RequestID, responseBody: JsValue) = {
    log error s"Failing request '$requestID', response '$responseBody'."
    failExceptionally(requestID, new RequestFailure(responseBody))
  }

  def failExceptionally(requestID: RequestID, t: Throwable) =
    baseComplete(requestID)(_.tryFailure(t))

  private def baseComplete(request: RequestID)(f: Promise[JsValue] => Unit): Boolean =
    (ongoing get request).exists { promise =>
      f(promise)
      ongoing -= request
      true
    }

}
