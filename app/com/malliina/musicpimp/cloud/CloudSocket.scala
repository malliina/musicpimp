package com.malliina.musicpimp.cloud

import java.nio.file.{Files, Path}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{MusicPlayer, PlaybackMessageHandler}
import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudSocket.{hostPort, httpProtocol}
import com.malliina.musicpimp.cloud.CloudStrings.{BODY, REGISTERED, REQUEST_ID, SUCCESS, UNREGISTER}
import com.malliina.musicpimp.cloud.PimpMessages._
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.http.{HttpConstants, MultipartRequest, TrustAllMultipartRequest}
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.{Library, MusicLibrary, PlaylistService, PlaylistSubmission}
import com.malliina.musicpimp.models.{PlaylistID, RequestID, User}
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.play.json.JsonStrings.CMD
import com.malliina.play.json.SimpleCommand
import com.malliina.rx.Observables
import com.malliina.security.SSLUtils
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.util.{Log, Util}
import com.malliina.ws.HttpUtil
import controllers.{Alarms, LibraryController, Rest}
import play.api.libs.json._

import scala.concurrent.duration.DurationInt
import scala.concurrent.{Future, Promise}
import scala.util.Try

case class Deps(playlists: PlaylistService,
                db: PimpDb,
                userManager: UserManager[User, String],
                handler: PlaybackMessageHandler,
                lib: MusicLibrary)

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
  *
  * @author Michael
  */
class CloudSocket(uri: String, username: String, password: String, deps: Deps)
  extends JsonSocket8(uri, SSLUtils.trustAllSslContext(), HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username, password))
  with Log {
  val lib = deps.lib
  val handler = deps.handler
  private val registrationPromise = Promise[String]()
  val registration = registrationPromise.future
  val playlists = deps.playlists

  def connectID(): Future[String] = connect().flatMap(_ => registration)

  def unregister() = Try(sendMessage(SimpleCommand(UNREGISTER)))

  /**
    * Reconnections are currently not supported; only call this method once per instance.
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
        (json \ REQUEST_ID).validate[RequestID].map(request => {
          sendFailureResponse(JsonMessages.failure(s"The MusicPimp server failed while dealing with the request: $json"), request)
        })
    }
  }

  protected def processRequest(json: JsValue): JsResult[Unit] = parseRequest(json) map (pair => handleRequest(pair._1, pair._2))

  protected def processEvent(json: JsValue): JsResult[Unit] = parseEvent(json) map handleEvent

  protected def parseRequest(json: JsValue): JsResult[(PimpMessage, RequestID)] = {
    val cmd = (json \ CMD).validate[String]
    val request = (json \ REQUEST_ID).validate[RequestID]
    val user = (json \ USERNAME).validate[User]
    val body = json \ BODY

    def withBody(f: JsValue => PimpMessage): JsResult[PimpMessage] = body.toOption
      .map(js => JsSuccess(f(js)))
      .getOrElse(JsError(s"Key $BODY does not contain JSON."))

    def withUser[T](transform: User => JsResult[T]): JsResult[T] = user.flatMap(transform)

    val requestMessage: JsResult[PimpMessage] = request.flatMap(req => cmd.flatMap {
      case VERSION => JsSuccess(GetVersion)
      case TRACK => body.validate[RangedTrack] orElse body.validate[Track] // the fallback is not needed I think
      case META => body.validate[GetMeta]
      case PING => JsSuccess(Ping)
      case AUTHENTICATE => body.validate[Authenticate]
      case ROOT_FOLDER => JsSuccess(RootFolder)
      case FOLDER => body.validate[Folder]
      case SEARCH => body.validate[Search]
      case PlaylistsGet => user.map(u => GetPlaylists(u))
      case PlaylistGet => withUser(u => (body \ ID).validate[PlaylistID].map(GetPlaylist(_, u)))
      case PlaylistSave => withUser(u => (body \ PlaylistKey).validate[PlaylistSubmission].map(SavePlaylist(_, u)))
      case PlaylistDelete => withUser(u => (body \ ID).validate[PlaylistID].map(DeletePlaylist(_, u)))
      case ALARMS => JsSuccess(GetAlarms)
      case ALARMS_EDIT => withBody(AlarmEdit.apply)
      case ALARMS_ADD => withBody(AlarmAdd.apply)
      case BEAM => body.validate[BeamCommand]
      case STATUS => JsSuccess(GetStatus)
      case other => JsError(s"Unknown JSON command: $other in $json")
    })
    request.flatMap(req => requestMessage.map(msg => (msg, req)))
  }

  protected def parseEvent(json: JsValue): JsResult[PimpMessage] = {
    val event = (json \ CMD).validate[String].orElse((json \ EVENT).validate[String])
    val body = json \ BODY
    val eventMessage = event.flatMap {
      case REGISTERED =>
        body.validate[Registered]
      case PLAYER =>
        body.toOption
          .map(bodyJson => JsSuccess(PlaybackMessage(bodyJson)))
          .getOrElse(JsError(s"Playback message does not contain JSON in key $BODY."))
      case PING =>
        JsSuccess(Ping)
      case other =>
        JsError(s"Unknown JSON event: $other in $json")
    }
    eventMessage
  }

  def handleRequest(message: PimpMessage, request: RequestID): Unit = {
    message match {
      case GetStatus =>
        sendJsonResponse(JsonMessages.withStatus(Json.toJson(MusicPlayer.status)), request)
      case t: Track =>
        upload(t, request).recoverAll(t => log.error("Upload failed", t))
      case rt: RangedTrack =>
        rangedUpload(rt, request).recoverAll(t => log.error("Ranged upload failed", t))
      case RootFolder =>
        val result = lib.rootFolder
        result.map(folder => sendResponse(folder, request))
        result.recover {
          case t =>
            log.error(s"Root folder failure", t)
            sendJsonResponse(JsonMessages.failure("Library failure"), request, success = false)
        }
      case Folder(id) =>
        val folderFuture = lib.folder(id).recover {
          case t =>
            log.error(s"Library failure for folder $id", t)
            None
        }
        folderFuture.map(maybeFolder => {
          val json = maybeFolder
            .map(folder => Json.toJson(folder))
            .getOrElse(JsonMessages.failure(s"Folder not found: $id"))
          sendJsonResponse(json, request, success = maybeFolder.isDefined)
        })
      case Search(term, limit) =>
        val result = deps.db.fullText(term, limit)
        result.map(tracks => sendResponse(tracks, request))
      case PingAuth =>
        sendJsonResponse(JsonMessages.version, request)
      case Ping =>
        sendJsonResponse(JsonMessages.ping, request)
      case GetPlaylists(user) =>
        withDatabaseExcuse(request) {
          playlists.playlistsMeta(user)
            .map(pls => sendResponse(pls, request))
        }
      case GetPlaylist(id, user) =>
        def notFound = JsonMessages.failure(s"Playlist not found: $id")
        withDatabaseExcuse(request) {
          playlists.playlistMeta(id, user).map(maybePlaylist => {
            maybePlaylist.fold(sendFailureResponse(notFound, request))(pl => sendResponse(pl, request))
          })
        }
      case SavePlaylist(playlist, user) =>
        withDatabaseExcuse(request) {
          playlists.saveOrUpdatePlaylistMeta(playlist, user).map(meta => sendResponse(meta, request))
        }
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
        authentication.map(isValid => {
          val response =
            if (isValid) JsonMessages.version
            else JsonMessages.invalidCredentials
          sendJsonResponse(response, request, success = isValid)
        })
      case GetVersion =>
        sendJsonResponse(JsonMessages.version, request)
      case GetMeta(id) =>
        val metaResult = LibraryController.trackMetaJson(id)
        val response = metaResult getOrElse LibraryController.noTrackJson(id)
        sendJsonResponse(response, request, metaResult.isDefined)
      case RegistrationEvent(event, id) =>
        registrationPromise trySuccess id
      case PlaybackMessage(payload) =>
        handler handleMessage payload
      case beamCommand: BeamCommand =>
        Future(Rest beam beamCommand)
          .map(e => e.fold(err => log.warn(s"Unable to beam. $err"), _ => log info "Beaming completed successfully."))
          .recoverAll(t => log.warn(s"Beaming failed.", t))
        sendAckResponse(request)
      case other =>
        log.warn(s"Unknown request: $message")
    }
  }

  def withDatabaseExcuse[T](request: RequestID)(f: Future[T]) = {
    f.recoverAll(t => {
      log.error(s"Request $request error", t)
      sendFailureResponse(JsonMessages.databaseFailure, request)
    })
  }

  def handleEvent(e: PimpMessage): Unit = {
    e match {
      case Registered(id) => registrationPromise trySuccess id
      case RegistrationEvent(event, id) => registrationPromise trySuccess id
      case PlaybackMessage(payload) => handler handleMessage payload
      case Ping => ()
      case other => log.warn(s"Unknown event: $other")
    }
  }

  def requestJson(request: RequestID, success: Boolean) = {
    Json.obj(
      REQUEST_ID -> request,
      SUCCESS -> success,
      BODY -> Json.obj())
  }

  def sendResponse[T](response: T, request: RequestID)(implicit writer: Writes[T]): Try[Unit] =
    sendJsonResponse(Json.toJson(response), request)

  def sendDefaultFailure(request: RequestID) = {
    sendFailureResponse(JsonMessages.genericFailure, request)
  }

  def sendFailureResponse(response: JsValue, request: RequestID) = {
    sendJsonResponse(response, request, success = false)
  }

  def sendAckResponse(request: RequestID) = sendJsonResponse(Json.obj(), request, success = true)

  def sendJsonResponse(response: JsValue, request: RequestID, success: Boolean = true) = {
    val payload = requestJson(request, success) + (BODY -> response)
    sendLogged(payload, request)
  }

  def sendLogged(payload: JsValue, request: RequestID): Try[Unit] = {
    send(payload)
      .map(_ => log debug s"Responded to request: $request with payload: $payload")
      .recover {
        case t => log.error(s"Unable to send message to the cloud, payload: $payload", t)
      }
  }

  def handleError(errors: JsError, json: JsValue): Unit = log warn errorMessage(errors, json)

  def errorMessage(errors: JsError, json: JsValue): String = {
    s"JSON error: $errors. Message: $json"
  }

  override def onClose(): Unit = failRegistration(CloudSocket.connectionClosed)

  override def onError(e: Exception): Unit = failRegistration(e)

  def failRegistration(e: Exception) = registrationPromise tryFailure e

  /**
    * Uploads `track` to the cloud. Sets `request` in the `REQUEST_ID` header and uses this server's ID as the username.
    *
    * @param track track to upload
    * @param request request id
    * @return
    */
  def upload(track: Track, request: RequestID): Future[Unit] = {
    withUpload(track.id, request, file => Files.size(file).bytes, (file, req) => req.addFile(file))
  }

  def rangedUpload(rangedTrack: RangedTrack, request: RequestID): Future[Unit] = {
    val range = rangedTrack.range
    withUpload(rangedTrack.id, request, _ => range.contentSize, (file, req) => {
      if (range.isAll) {
        log info s"Uploading $file, request $request"
        req.addFile(file)
      } else {
        log info s"Uploading $file, range $range, request $request"
        req.addRangedFile(file, range)
      }
    })
  }

  private def withUpload(trackID: String, request: RequestID, sizeCalc: Path => StorageSize, content: (Path, MultipartRequest) => Unit): Future[Unit] = {
    val uploadUri = s"$httpProtocol://$hostPort/track"
    val trackOpt = Library.findAbsolute(trackID)
    if (trackOpt.isEmpty) {
      log warn s"Unable to find track: $trackID"
    }
    Future {
      def appendMeta(message: String) = s"$message. URI: $uploadUri. Request: $request."
      Util.using(new TrustAllMultipartRequest(uploadUri))(req => {
        req.addHeaders(REQUEST_ID -> request.id)
        Clouds.loadID().foreach(id => req.setAuth(id, "pimp"))
        trackOpt.foreach(path => {
          content(path, req)
        })
        val response = req.execute()
        val code = response.getStatusLine.getStatusCode
        val isSuccess = code >= 200 && code < 300
        if (!isSuccess) {
          log error appendMeta(s"Non-success response code $code for track ID $trackID")
        } else {
          val prefix = trackOpt.map(f => s"Uploaded ${sizeCalc(f)} of $trackID").getOrElse("Uploaded no bytes")
          log info appendMeta(s"$prefix with response $code")
        }
      })
    }
  }
}

object CloudSocket {
  val isDev = false
  val (hostPort, httpProtocol, socketProtocol) =
    if (isDev) ("localhost:9000", "http", "ws")
    else ("cloud.musicpimp.org", "https", "wss")

  def build(id: Option[String], deps: Deps) = {
    new CloudSocket(s"$socketProtocol://$hostPort/servers/ws2", id getOrElse "", "pimp", deps)
  }

  val notConnected = new Exception("Not connected.")
  val connectionClosed = new Exception("Connection closed.")
}
