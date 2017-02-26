package com.malliina.ws

import java.util.UUID

import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.ws.IterateeStore.log
import play.api.Logger
import play.api.libs.iteratee.Concurrent.Channel
import play.api.libs.iteratee.{Concurrent, Enumerator, Iteratee}
import play.api.libs.json.{JsValue, Json}
import rx.lang.scala.subjects.BehaviorSubject

import scala.collection.concurrent.TrieMap
import scala.util.{Failure, Success, Try}

class IterateeStore[T] {
  private val ongoingRequests = TrieMap.empty[UUID, IterateeInfo]
  val uuids = BehaviorSubject(Map.empty[UUID, IterateeInfo])

  def send(message: JsValue, channel: Channel[JsValue], owner: String): Option[Enumerator[T]] = {
    val (iteratee, enumerator) = Concurrent.joined[T]
    val uuid = UUID.randomUUID()
    ongoingRequests += (uuid -> IterateeInfo(iteratee, owner))
    updateObservable()
    val payload = Json.obj(RequestId -> uuid.toString, Body -> message)
    log debug s"Sending request: $uuid with body: $message"
    val ret = Try(channel push payload)
    ret match {
      case Success(()) =>
        Some(enumerator)
      case Failure(t) =>
        log.warn(s"Unable to send payload: $payload", t)
        remove(uuid)
        // close something?
        // iteratee.feed(Input.EOF)
        None
    }
  }

  def remove(uuid: UUID): Option[IterateeInfo] = {
    val ret = ongoingRequests remove uuid
    updateObservable()
    ret
  }

  def exists(uuid: UUID) = ongoingRequests contains uuid

  def get(uuid: UUID): Option[Iteratee[T, Unit]] = (ongoingRequests get uuid) map (_.iteratee)

  private def updateObservable(): Unit = uuids onNext ongoingRequests.toMap

  case class IterateeInfo(iteratee: Iteratee[T, Unit], owner: String)

}

object IterateeStore {
  private val log = Logger(getClass)
}
