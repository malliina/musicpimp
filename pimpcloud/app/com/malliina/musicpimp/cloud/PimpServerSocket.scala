package com.malliina.musicpimp.cloud

import akka.actor.{ActorRef, Scheduler}
import akka.stream.Materializer
import com.malliina.concurrent.Execution.cached
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.pimpcloud.ws.NoCacheByteStreams
import com.malliina.play.ContentRange
import com.malliina.values.{Password, Username}
import com.malliina.ws.{JsonFutureSocket, Streamer}
import play.api.http.HttpErrorHandler
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object PimpServerSocket {
  val DefaultSearchLimit = 100
  val nobody = Username("nobody")
}

/**
  * @param jsonOut send messages to this actor to send messages to the server
  * @param id      the cloud ID of the server
  */
class PimpServerSocket(
  val jsonOut: ActorRef,
  id: CloudID,
  val headers: RequestHeader,
  mat: Materializer,
  scheduler: Scheduler,
  errorHandler: HttpErrorHandler,
  onUpdate: () => Unit
) extends JsonFutureSocket(id, scheduler) {
  val fileTransfers: Streamer = new NoCacheByteStreams(id, jsonOut, mat, errorHandler, onUpdate)

  override def send(payload: JsValue): Unit = jsonOut ! payload

  def requestTrack(track: Track, contentRange: ContentRange, req: RequestHeader): Result =
    fileTransfers.requestTrack(track, contentRange, req)

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): Future[Boolean] =
    authenticateWithVersion(user, pass).map(_ => true).recover { case _ => false }

  def authenticateWithVersion(user: Username, pass: Password): Future[JsResult[Version]] = {
    val req = PhoneRequest(AuthenticateKey, PimpServerSocket.nobody, Authenticate(user, pass))
    proxyValidated[Authenticate, Version](req)
  }
}
