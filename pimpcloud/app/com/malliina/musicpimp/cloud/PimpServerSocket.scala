package com.malliina.musicpimp.cloud

import akka.actor.ActorRef
import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{Directory, Track}
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models._
import com.malliina.pimpcloud.json.JsonStrings._
import com.malliina.pimpcloud.models._
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

  def pingAuth(user: Username): Future[Version] =
    proxied[Version](VersionKey, user)

  def meta(id: TrackID, user: Username): Future[Track] =
    makeRequest[GetMeta, Track](Meta, user, GetMeta(id))

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: Username, pass: Password): Future[Version] =
    makeRequest[Authenticate, Version](AuthenticateKey, PimpServerSocket.nobody, Authenticate(user, pass))

  def rootFolder(user: Username) = proxied[Directory](RootFolderKey, user)

  def folder(id: FolderID, user: Username) =
    makeRequest[GetFolder, Directory](FolderKey, user, GetFolder(id))

  def search(term: String, user: Username, limit: Int = PimpServerSocket.DefaultSearchLimit) =
    makeRequest[Search, Seq[Track]](SearchKey, user, Search(term, limit))

  def status(user: Username) = proxied[JsValue](StatusKey, user)

  def makeRequest[W: Writes, R: Reads](cmd: String, user: Username, t: W): Future[R] =
    proxyT[W, R](cmd, user, t)

  protected def proxied[T: Reads](cmd: String, user: Username) =
    proxyD[JsValue, T](PhoneRequest(cmd, user, Json.obj()))
}
