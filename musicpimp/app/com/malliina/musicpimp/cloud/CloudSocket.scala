package com.malliina.musicpimp.cloud

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.http.FullUrl
import com.malliina.logstreams.client.CustomSSLSocketFactory
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudSocket.log
import com.malliina.musicpimp.cloud.CloudStrings.Unregister
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.http.HttpConstants
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.library._
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.musicpimp.stats.{PlaybackStats, PopularList, RecentList}
import com.malliina.play.models.{Password, Username}
import com.malliina.rx.Observables
import com.malliina.ws.HttpUtil
import controllers.musicpimp.{LibraryController, Rest}
import play.api.Logger
import play.api.libs.json._
import rx.lang.scala.Subject

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.Try

case class Deps(playlists: PlaylistService,
                db: PimpDb,
                userManager: UserManager[Username, Password],
                handler: PlaybackMessageHandler,
                lib: MusicLibrary,
                stats: PlaybackStats)

object CloudSocket {
  private val log = Logger(getClass)

  val path = "/servers/ws"
  val devUri = FullUrl("ws", "localhost:9000", path)
  val prodUri = FullUrl("wss", "cloud.musicpimp.org", path)

  def build(id: Option[CloudID], url: FullUrl, deps: Deps) = {
    new CloudSocket(url, id getOrElse CloudID.empty, Password("pimp"), deps)
  }

  val notConnected = new Exception("Not connected.")
  val connectionClosed = new Exception("Connection closed.")
  val manuallyClosed = new Exception("Connection closed manually.")
}

/**
  * Event format:
  *
  * {
  * "cmd": "...",
  * "request": "...",
  * "body": "..."
  * }
  *
  * or
  *
  * {
  * "event": "...",
  * "body": "..."
  * }
  *
  * Key cmd or event must exist. Key request is defined if a response is desired. Key body may or may not exist, depending on cmd.
  */
class CloudSocket(uri: FullUrl, username: CloudID, password: Password, deps: Deps)
  extends JsonSocket8(
    uri,
    CustomSSLSocketFactory.forHost("cloud.musicpimp.org"),
    HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username.id, password.pass)) {

  val messageParser = CloudMessageParser
  val httpProto = if (uri.proto == "ws") "http" else "https"
  val cloudHost = FullUrl(httpProto, uri.hostAndPort, "")
  val uploader = TrackUploads(cloudHost)
  val lib = deps.lib
  val handler = deps.handler
  val stats = deps.stats
  private val registrationPromise = Promise[CloudID]()
  val registration = registrationPromise.future
  val playlists = deps.playlists

  val registrations = Subject[CloudID]().toSerialized

  def connectID(): Future[CloudID] = connect().flatMap(_ => registration)

  def unregister() = Try(sendMessage(SimpleCommand(Unregister)))

  /** Reconnections are currently not supported; only call this method once per instance.
    *
    * Impl: On subsequent calls, the returned future will always be completed regardless of connection result
    *
    * @return a future that completes when the connection has successfully been established
    */
  override def connect(): Future[Unit] = {
    log info s"Connecting as '$username' to $uri..."
    Observables.timeoutAfter(10.seconds, registrationPromise)
    super.connect()
  }

  override def onMessage(json: JsValue): Unit = {
    log debug s"Got message: '$json'."
    try {
      // attempts to handle the message as a request, then if that fails as an event, if all fails handles the error
      processRequest(json) orElse processEvent(json) recoverTotal (err => handleError(err, json))
    } catch {
      case e: Exception =>
        log.warn(s"Failed while handling JSON: '$json'.", e)
        (json \ CloudResponse.RequestKey).validate[RequestID] map { request =>
          val reason = FailReason(s"The MusicPimp server failed while dealing with the request: '$json'.")
          sendFailure(request, reason)
        }
    }
  }

  protected def processRequest(json: JsValue): JsResult[Unit] =
    parseRequest(json) map (request => handleRequest(request))

  protected def processEvent(json: JsValue): JsResult[Unit] =
    parseEvent(json) map handleEvent

  protected def parseRequest(json: JsValue): JsResult[CloudRequest] =
    messageParser.parseRequest(json)

  protected def parseEvent(json: JsValue): JsResult[PimpMessage] =
    messageParser.parseEvent(json)

  def handleRequest(cloudRequest: CloudRequest): Unit = {
    val request = cloudRequest.request
    val message = cloudRequest.message

    def databaseResponse[T: Writes](f: Future[T]): Future[Any] =
      withDatabaseExcuse(request)(f.map(t => sendSuccess(request, t)))

    message match {
      case GetStatus =>
        sendSuccess(request, StatusMessage(MusicPlayer.status(cloudHost)))
      case GetTrack(id) =>
        uploader.upload(id, request).recoverAll { t =>
          log.error(s"Upload failed for $request", t)
        }
      case rt: RangedTrack =>
        uploader.rangedUpload(rt, request).recoverAll { t =>
          log.error(s"Ranged upload failed for $request", t)
        }
      case CancelStream(req) =>
        uploader.cancelSoon(req)
      case RootFolder =>
        lib.rootFolder.map { folder =>
          sendSuccess(request, folder.toFull(cloudHost))
        }.recoverAll { t =>
          log.error(s"Root folder failure.", t)
          sendFailure(request, FailReason("Library failure."))
        }
      case GetFolder(id) =>
        lib.folder(id).map { maybeFolder =>
          maybeFolder.map { folder =>
            sendSuccess(request, folder.toFull(cloudHost))
          }.getOrElse {
            val msg = s"Folder not found: '$id'."
            log.warn(msg)
            sendFailure(request, FailReason(msg))
          }
        } recoverAll { t =>
          val msg = s"Library failure for folder '$id'."
          log.error(msg, t)
          sendFailure(request, FailReason(msg))
        }
      case Search(term, limit) =>
        databaseResponse(deps.db.fullText(term, limit))
      case PingAuth =>
        sendSuccess(request, JsonMessages.version)
      case PingMessage =>
        sendSuccess(request, PingEvent)
      case GetPopular(meta) =>
        databaseResponse(stats.mostPlayed(meta).map(PopularList.forEntries(meta, _, cloudHost)))
      case GetRecent(meta) =>
        databaseResponse(stats.mostRecent(meta).map(RecentList.forEntries(meta, _, cloudHost)))
      case GetPlaylists(user) =>
        databaseResponse(playlists.playlistsMeta(user).map(TrackJson.toFullPlaylistsMeta(_, cloudHost)))
      case GetPlaylist(id, user) =>
        withDatabaseExcuse(request) {
          playlists.playlistMeta(id, user).map { maybePlaylist =>
            maybePlaylist.map { playlist =>
              sendSuccess(request, TrackJson.toFullMeta(playlist, cloudHost))
            }.getOrElse {
              sendFailure(request, FailReason(s"Playlist not found: '$id'."))
            }
          }
        }
      case SavePlaylist(playlist, user) =>
        databaseResponse(playlists.saveOrUpdatePlaylistMeta(playlist, user))
      case DeletePlaylist(id, user) =>
        withDatabaseExcuse(request) {
          playlists.delete(id, user).map { _ =>
            sendLogged(CloudResponse.ack(request))
          }
        }
      case GetAlarms =>
        sendLogged(CloudResponse.success(request, ScheduledPlaybackService.status.map(_.toFull(cloudHost))))
      case AlarmEdit(payload) =>
        AlarmJsonHandler.handleCommand(payload)
        sendSuccess(request, Json.obj())
      case AlarmAdd(payload) =>
        AlarmJsonHandler.handleCommand(payload)
        sendSuccess(request, Json.obj())
      case Authenticate(user, pass) =>
        val authentication = deps.userManager.authenticate(user, pass).recover {
          case t =>
            log.error(s"Database failure when authenticating '$user'.", t)
            false
        }
        authentication map { isValid =>
          if (isValid) sendSuccess(request, JsonMessages.version)
          else sendFailure(request, JsonMessages.invalidCredentials)
        }
      case GetVersion =>
        sendSuccess(request, JsonMessages.version)
      case GetMeta(id) =>
        Library.findMeta(id)
          .map(track => sendSuccess(request, TrackJson.toFull(track, cloudHost)))
          .getOrElse(sendFailure(request, LibraryController.noTrackJson(id)))
      case RegistrationEvent(_, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handlePlayerMessage(payload, user)
      case beamCommand: BeamCommand =>
        Future(Rest beam beamCommand)
          .map(e => e.fold(err => log.warn(s"Unable to beam. $err"), _ => log info "Beaming completed successfully."))
          .recoverAll(t => log.warn(s"Beaming failed.", t))
        sendLogged(CloudResponse.ack(request))
      case _ =>
        sendFailure(request, FailReason(s"Unknown message in request '$request'."))
        log.warn(s"Unknown request: '$message'.")
    }
  }

  def withDatabaseExcuse[T](request: RequestID)(f: Future[T]) =
    f.recoverAll { t =>
      log.error(s"Request $request error.", t)
      sendFailure(request, JsonMessages.databaseFailure)
    }

  def handleEvent(e: PimpMessage): Unit =
    e match {
      case RegisteredMessage(id) =>
        onRegistered(id)
      case RegistrationEvent(_, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handlePlayerMessage(payload, user)
      case PingMessage =>
        ()
      case other =>
        log.warn(s"Unknown event: '$other'.")
    }

  def handlePlayerMessage(message: PlayerMessage, user: Username): Unit = {
    handler.updateUser(user)
    handler.fulfillMessage(message, RemoteInfo.cloud(user, cloudHost))
  }

  def sendSuccess[T: Writes](request: RequestID, response: T) =
    sendLogged(CloudResponse.success(request, response))

  def sendFailure(request: RequestID, reason: FailReason) =
    sendLogged(CloudResponse.failed(request, reason))

  def sendLogged[T: Writes](response: CloudResponse[T]): Try[Unit] = {
    val request = response.request
    send(Json.toJson(response))
      .map(_ => log debug s"Responded to request $request with payload '$response'.")
      .recover {
        case t => log.error(s"Unable to respond to $request with payload '$response'.", t)
      }
  }

  def handleError(errors: JsError, json: JsValue): Unit =
    log warn errorMessage(errors, json)

  def errorMessage(errors: JsError, json: JsValue): String =
    s"JSON error: $errors. Message: $json"

  def onRegistered(id: CloudID): Unit = {
    registrationPromise trySuccess id
    registrations onNext id
    log info s"Connected as '$username' to $uri."
  }

  override def onClose(): Unit = {
    failSocket(CloudSocket.connectionClosed)
    log info s"Disconnected as '$username' from $uri."
  }

  override def onError(e: Exception): Unit = {
    failSocket(e)
  }

  override def close(): Unit = {
    failSocket(CloudSocket.manuallyClosed)
    uploader.close()
    super.close()
  }

  def failSocket(e: Exception) = {
    registrationPromise tryFailure e
    registrations onError e
  }
}
