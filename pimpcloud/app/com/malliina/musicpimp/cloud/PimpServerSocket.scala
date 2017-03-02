package com.malliina.musicpimp.cloud

import akka.actor.ActorRef
import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.Track
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models.PhoneRequest
import com.malliina.pimpcloud.ws.NoCacheByteStreams
import com.malliina.play.ContentRange
import com.malliina.play.models.{Password, Username}
import com.malliina.ws.{JsonFutureSocket, Streamer}
import play.api.libs.json._
import play.api.mvc.{RequestHeader, Result}

import scala.concurrent.Future

object PimpServerSocket {
  val DefaultSearchLimit = 100
  val nobody = Username("nobody")
}

/**
  * @param id the cloud ID of the server
  */
class PimpServerSocket(val jsonOut: ActorRef,
                       id: CloudID,
                       val headers: RequestHeader,
                       mat: Materializer,
                       onUpdate: () => Unit)
  extends JsonFutureSocket(id) {
  val fileTransfers: Streamer = new NoCacheByteStreams(id, jsonOut, mat, onUpdate)

  override def send(payload: JsValue) = jsonOut ! payload

  def requestTrack(track: Track, contentRange: ContentRange, req: RequestHeader): Result =
    fileTransfers.requestTrack(track, contentRange, req)

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): Future[Boolean] =
    authenticateWithVersion(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticateWithVersion(user: Username, pass: Password): Future[JsResult[Version]] = {
    val req = PhoneRequest(AuthenticateKey, PimpServerSocket.nobody, Authenticate(user, pass))
    proxyValidated[Authenticate, Version](req)
  }
}
