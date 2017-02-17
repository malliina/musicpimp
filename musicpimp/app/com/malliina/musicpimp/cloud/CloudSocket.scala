package com.malliina.musicpimp.cloud

import javax.net.ssl.SNIHostName

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio._
import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudSocket.{hostPort, httpProtocol, log}
import com.malliina.musicpimp.cloud.CloudStrings.{Body, RequestId, SuccessKey, Unregister}
import com.malliina.musicpimp.cloud.PimpMessages._
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.http.{CustomSSLSocketFactory, HttpConstants}
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.library._
import com.malliina.musicpimp.models._
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.musicpimp.stats.{PlaybackStats, PopularList, RecentList}
import com.malliina.play.json.SimpleCommand
import com.malliina.play.models.{Password, Username}
import com.malliina.rx.Observables
import com.malliina.ws.HttpUtil
import controllers.musicpimp.{Alarms, LibraryController, Rest}
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

  val isDev = false
  val (hostPort, httpProtocol, socketProtocol) =
    if (isDev) ("localhost:9000", "http", "ws")
    else ("cloud.musicpimp.org", "https", "wss")

  def build(id: Option[CloudID], deps: Deps) = {
    val url = PimpUrl(socketProtocol, hostPort, "/servers/ws2")
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
class CloudSocket(uri: PimpUrl, username: CloudID, password: Password, deps: Deps)
  extends JsonSocket8(
    uri,
    CustomSSLSocketFactory.withSNI(SNIHostName.createSNIMatcher("cloud\\.musicpimp\\.org"), new SNIHostName(uri.host)),
    HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username.id, password.pass)) {

  val messageParser = CloudMessageParser
  val cloudHost = PimpUrl(httpProtocol, hostPort, "")
  val uploader = TrackUploads(cloudHost)
  log info s"Initializing cloud connection with user $username"
  implicit val musicFolderWriter = MusicFolder.writer(cloudHost)
  implicit val trackWriter = TrackJson.format(cloudHost)

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
    log debug s"Connecting as user: $username"
    Observables.timeoutAfter(10.seconds, registrationPromise)
    super.connect()
  }

  override def onMessage(json: JsValue): Unit = {
    log debug s"Got message: $json"
    try {
      // attempts to handle the message as a request, then if that fails as an event, if all fails handles the error
      processRequest(json) orElse processEvent(json) recoverTotal (err => handleError(err, json))
    } catch {
      case e: Exception =>
        log.warn(s"Failed while handling JSON: $json.", e)
        (json \ RequestId).validate[RequestID] map { request =>
          sendFailureResponse(FailReason(s"The MusicPimp server failed while dealing with the request: $json"), request)
        }
    }
  }

  protected def processRequest(json: JsValue): JsResult[Unit] =
    parseRequest(json) map (pair => handleRequest(pair._1, pair._2))

  protected def processEvent(json: JsValue): JsResult[Unit] =
    parseEvent(json) map handleEvent

  protected def parseRequest(json: JsValue): JsResult[(PimpMessage, RequestID)] =
    messageParser.parseRequest(json)

  protected def parseEvent(json: JsValue): JsResult[PimpMessage] =
    messageParser.parseEvent(json)

  def handleRequest(message: PimpMessage, request: RequestID): Unit = {

    def databaseResponse[T: Writes](f: Future[T]): Future[Any] =
      withDatabaseExcuse(request)(f.map(t => sendResponse(t, request)))

    message match {
      case GetStatus =>
        val payload = Json.toJson(MusicPlayer.status)(StatusEvent.status18writer)
        sendJsonResponse(JsonMessages.withStatus(payload), request)
      case t: Track =>
        uploader.upload(t, request).recoverAll(t => log.error(s"Upload failed for $request", t))
      case rt: RangedTrack =>
        uploader.rangedUpload(rt, request).recoverAll(t => log.error(s"Ranged upload failed for $request", t))
      case CancelStream(req) =>
        uploader.cancelSoon(req)
      case RootFolder =>
        lib.rootFolder map (folder => sendResponse(folder, request)) recover {
          case t =>
            log.error(s"Root folder failure", t)
            sendJsonResponse(JsonMessages.failure("Library failure"), request, success = false)
        }
      case GetFolder(id) =>
        val folderFuture = lib.folder(id) recover {
          case t =>
            log.error(s"Library failure for folder $id", t)
            None
        }
        folderFuture map { maybeFolder =>
          val json = maybeFolder
            .map(folder => Json.toJson(folder))
            .getOrElse(JsonMessages.failure(s"Folder not found: $id"))
          sendJsonResponse(json, request, success = maybeFolder.isDefined)
        }
      case Search(term, limit) =>
        databaseResponse(deps.db.fullText(term, limit))
      case PingAuth =>
        sendJsonResponse(Json.toJson(JsonMessages.version), request)
      case PingMessage =>
        sendJsonResponse(JsonMessages.ping, request)
      case GetPopular(meta) =>
        databaseResponse(stats.mostPlayed(meta).map(PopularList.apply))
      case GetRecent(meta) =>
        databaseResponse(stats.mostRecent(meta).map(RecentList.apply))
      case GetPlaylists(user) =>
        databaseResponse(playlists.playlistsMeta(user))
      case GetPlaylist(id, user) =>
        def notFound = FailReason(s"Playlist not found: $id")

        withDatabaseExcuse(request) {
          playlists.playlistMeta(id, user).map { maybePlaylist =>
            maybePlaylist.fold(sendFailureResponse(notFound, request))(pl => sendResponse(pl, request))
          }
        }
      case SavePlaylist(playlist, user) =>
        databaseResponse(playlists.saveOrUpdatePlaylistMeta(playlist, user))
      case DeletePlaylist(id, user) =>
        withDatabaseExcuse(request) {
          playlists.delete(id, user).map(_ => sendAckResponse(request))
        }
      case GetAlarms =>
        implicit val writer = Alarms.alarmWriter
        sendResponse(ScheduledPlaybackService.status, request)
      case AlarmEdit(payload) =>
        AlarmJsonHandler.handle(payload)
        sendAckResponse(request)
      case AlarmAdd(payload) =>
        AlarmJsonHandler.handle(payload)
        sendAckResponse(request)
      case Authenticate(user, pass) =>
        val authentication = deps.userManager.authenticate(user, pass).recover {
          case t =>
            log.error(s"Database failure when authenticating $user", t)
            false
        }
        authentication map { isValid =>
          val response =
            if (isValid) Json.toJson(JsonMessages.version)
            else Json.toJson(JsonMessages.invalidCredentials)
          sendJsonResponse(response, request, success = isValid)
        }
      case GetVersion =>
        sendResponse(JsonMessages.version, request)
      case GetMeta(id) =>
        val metaResult = Library.findMeta(id).map(t => Json.toJson(t))
        val response = metaResult getOrElse LibraryController.noTrackJson(id)
        sendJsonResponse(response, request, metaResult.isDefined)
      case RegistrationEvent(event, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handler.handleMessage(payload, RemoteInfo(user, cloudHost))
      case beamCommand: BeamCommand =>
        Future(Rest beam beamCommand)
          .map(e => e.fold(err => log.warn(s"Unable to beam. $err"), _ => log info "Beaming completed successfully."))
          .recoverAll(t => log.warn(s"Beaming failed.", t))
        sendAckResponse(request)
      case other =>
        log.warn(s"Unknown request: $message")
    }
  }

  def withDatabaseExcuse[T](request: RequestID)(f: Future[T]) =
    f.recoverAll { t =>
      log.error(s"Request $request error", t)
      sendFailureResponse(JsonMessages.databaseFailure, request)
    }

  def handleEvent(e: PimpMessage): Unit =
    e match {
      case RegisteredMessage(id) =>
        onRegistered(id)
      case RegistrationEvent(event, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handler.handleMessage(payload, RemoteInfo(user, cloudHost))
      case PingMessage =>
        ()
      case other =>
        log.warn(s"Unknown event: $other")
    }

  def requestJson(request: RequestID, success: Boolean) =
    Json.obj(
      RequestId -> request,
      SuccessKey -> success,
      Body -> Json.obj())

  def sendResponse[T: Writes](response: T, request: RequestID): Try[Unit] =
    sendJsonResponse(Json.toJson(response), request)

  def sendDefaultFailure(request: RequestID) =
    sendFailureResponse(JsonMessages.genericFailure, request)

  def sendFailureResponse(response: FailReason, request: RequestID) =
    sendJsonResponse(Json.toJson(response), request, success = false)

  def sendAckResponse(request: RequestID) =
    sendJsonResponse(Json.obj(), request, success = true)

  def sendJsonResponse(response: JsValue, request: RequestID, success: Boolean = true) = {
    val payload = requestJson(request, success) + (Body -> response)
    sendLogged(payload, request)
  }

  def sendLogged(payload: JsValue, request: RequestID): Try[Unit] =
    send(payload)
      .map(_ => log debug s"Responded to request: $request with payload: $payload")
      .recover {
        case t => log.error(s"Unable to send message to the cloud, payload: $payload", t)
      }

  def handleError(errors: JsError, json: JsValue): Unit = log warn errorMessage(errors, json)

  def errorMessage(errors: JsError, json: JsValue): String =
    s"JSON error: $errors. Message: $json"

  def onRegistered(id: CloudID) = {
    registrationPromise trySuccess id
    registrations onNext id
  }

  override def onClose(): Unit = {
    failSocket(CloudSocket.connectionClosed)
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
