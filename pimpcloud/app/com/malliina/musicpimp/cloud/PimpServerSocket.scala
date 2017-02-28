package com.malliina.musicpimp.cloud

import akka.actor.ActorRef
import akka.stream.Materializer
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{Directory, Track}
import com.malliina.musicpimp.cloud.PimpServerSocket.nobody
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

  def idBody(id: Identifiable): JsObject = body(Id -> id.id)

  /**
    * @return a JSON object with parameter `cmd` in key `cmd` and dictionary `more` in key `body`
    */
  def body(more: (String, Json.JsValueWrapper)*): JsObject =
    Json.obj(more: _*)
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

  def pingAuth: Future[Version] =
    proxied[Version](nobody, VersionKey)

  def meta(id: TrackID): Future[Track] =
    makeRequest[GetMeta, Track](Meta, GetMeta(id))

  /**
    * @param user username
    * @param pass password
    * @return true if authentication succeeds, false if the credentials are bogus or any failure occurs
    */
  def authenticate(user: Username, pass: Password): Future[Boolean] =
    authenticate3(user, pass).map(_ => true).recoverAll(_ => false)

  def authenticate3(user: Username, pass: Password): Future[Version] =
    makeRequest[Authenticate, Version](AuthenticateKey, Authenticate(user, pass))

  def rootFolder = proxied[Directory](nobody, RootFolderKey)

  def folder(id: FolderID) =
    makeRequest[GetFolder, Directory](FolderKey, GetFolder(id))

  def search(term: String, limit: Int = PimpServerSocket.DefaultSearchLimit) =
    makeRequest[Search, Seq[Track]](SearchKey, Search(term, limit))

  def status = simpleProxy(StatusKey)

  def makeRequest[W: Writes, R: Reads](cmd: String, t: W): Future[R] =
    proxyT[W, R](cmd, t, nobody)

  protected def proxied[T: Reads](user: Username, cmd: String) =
    proxyD[T](PhoneRequest(cmd, Json.obj()), user)

  private def simpleProxy(cmd: String) = defaultProxy(PhoneRequest(cmd, Json.obj()), nobody)
}
