package com.malliina.musicpimp.cloud

import cats.effect.Async
import cats.effect.implicits.genTemporalOps_
import cats.effect.implicits.genTemporalOps
import cats.effect.kernel.Deferred
import cats.effect.std.Dispatcher
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxApplicativeId, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
import com.malliina.http.FullUrl
import com.malliina.http.io.HttpClientF2
import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudSocket.log
import com.malliina.musicpimp.cloud.CloudStrings.Unregister
import com.malliina.musicpimp.db.FullText
import com.malliina.musicpimp.http.{HttpConstants, Rest}
import com.malliina.musicpimp.http4s.LibraryController
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.library.*
import com.malliina.musicpimp.models.*
import com.malliina.musicpimp.scheduler.ScheduledPlaybackService
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.stats.{PlaybackStats, PopularList, RecentList}
import com.malliina.util.AppLogger
import com.malliina.values.{Password, Username}
import com.malliina.ws.HttpUtil
import fs2.concurrent.Topic
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}

import java.util.concurrent.TimeoutException
import scala.concurrent.duration.DurationInt
import scala.util.Try

case class Deps[F[_]](
  playlists: PlaylistService[F],
  userManager: UserManager[F, Username, Password],
  handler: PlaybackMessageHandler[F],
  lib: MusicLibrary[F],
  stats: PlaybackStats[F],
  schedules: ScheduledPlaybackService[F],
  http: HttpClientF2[F],
  d: Dispatcher[F]
)

object CloudSocket:
  private val log = AppLogger(getClass)

  val path = "/servers/ws"
  val devUri = FullUrl("ws", "localhost:9000", path)
  val prodUri = FullUrl("wss", "cloud.musicpimp.org", path)

  def build[F[_]: Async](
    player: MusicPlayer[F],
    id: Option[CloudID],
    url: FullUrl,
    handler: JsonHandler[F],
    fullText: FullText[F],
    deps: Deps[F]
  ): F[CloudSocket[F]] =
    for
      connPromise <- Deferred[F, Option[Throwable]]
      regPromise <- Deferred[F, Either[Exception, CloudID]]
      eventHub <- Topic[F, CloudID]
    yield CloudSocket(
      connPromise,
      regPromise,
      eventHub,
      deps.d,
      player,
      url,
      id.filter(_.id.nonEmpty).getOrElse(CloudID.empty),
      Constants.pass,
      handler,
      fullText,
      deps
    )

  val notConnected = new Exception("Not connected.")
  val connectionClosed = new Exception("Connection closed.")
  val manuallyClosed = new Exception("Connection closed manually.")

/** Event format:
  *
  * { "cmd": "...", "request": "...", "body": "..." }
  *
  * or
  *
  * { "event": "...", "body": "..." }
  *
  * Key cmd or event must exist. Key request is defined if a response is desired. Key body may or
  * may not exist, depending on cmd.
  */
class CloudSocket[F[_]: Async](
  connectPromise: Deferred[F, Option[Throwable]],
  registrationPromise: Deferred[F, Either[Exception, CloudID]],
  registrationsHub: Topic[F, CloudID],
  d: Dispatcher[F],
  player: MusicPlayer[F],
  uri: FullUrl,
  username: CloudID,
  password: Password,
  alarmHandler: JsonHandler[F],
  fullText: FullText[F],
  deps: Deps[F]
) extends JsonSocket8[F](
    uri,
    connectPromise,
    d,
    CustomSSLSocketFactory.forHost("cloud.musicpimp.org"),
    HttpConstants.AUTHORIZATION -> HttpUtil.authorizationValue(username.id, password.pass)
  ):
  private val messageParser = CloudMessageParser
  private val httpProto = if uri.proto == "ws" then "http" else "https"
  val cloudHost = FullUrl(httpProto, uri.hostAndPort, "")
//  val cloudHost = FullUrl("http", "10.0.0.2:9000", "")
  private val uploadHost = cloudHost
  val lib = deps.lib
  val uploader = ApacheTrackUploads(lib, uploadHost)
//  val uploader = OkHttpTrackUploads(lib, cloudHost)
  val handler = deps.handler
  val stats = deps.stats
  val playlists = deps.playlists

  val registrations = registrationsHub.subscribe(100)

  def registration: F[CloudID] = registrationPromise.get.flatMap: e =>
    e.fold(e => F.raiseError(e), ok => F.pure(ok))

  def connectID(): F[CloudID] = connect().flatMap(_ => registration)

  def unregister() = Try(sendMessage(SimpleCommand(Unregister)))

  /** Reconnections are currently not supported; only call this method once per instance.
    *
    * Impl: On subsequent calls, the returned future will always be completed regardless of
    * connection result
    *
    * @return
    *   a task that completes when the connection has successfully been established
    */
  override def connect(): F[Unit] =
    log.info(s"Connecting as '$username' to '$uri'...")
    val timeout = 10.seconds
    val timeoutTask = F
      .pure(())
      .delayBy(timeout)
      .flatMap: _ =>
        registrationPromise.complete(Left(TimeoutException(s"Timed out after $timeout.")))
    d.unsafeRunAndForget(timeoutTask)
    super.connect().timeout(10.seconds)

  override def onMessage(json: Json): F[Unit] =
    log.debug(s"Got message: '$json'.")
    // attempts to handle the message as a request, then if that fails as an event, if all fails handles the error
    processRequest(json)
      .orElse(processEvent(json))
      .fold(err => handleError(err, json), identity)
      .handleErrorWith: e =>
        log.warn(s"Failed while handling JSON: '$json'.", e)
        json.hcursor
          .downField(CloudResponse.RequestKey)
          .as[RequestID]
          .map: request =>
            val reason =
              FailReason(s"The MusicPimp server failed while dealing with the request: '$json'.")
            sendFailure(request, reason)
          .getOrElse:
            F.unit

  private def processRequest(json: Json): Either[DecodingFailure, F[Unit]] =
    messageParser.parseRequest(json).map(handleRequestTask)

  private def processEvent(json: Json): Either[DecodingFailure, F[Unit]] =
    messageParser.parseEvent(json).map(handleEvent)

  private def parseRequest(json: Json): Decoder.Result[CloudRequest] =
    messageParser.parseRequest(json)

  private def handleRequestTask(cloudRequest: CloudRequest): F[Unit] =
    val request = cloudRequest.request
    val message = cloudRequest.message

    def databaseResponse[T: Encoder](f: F[T]): F[Unit] =
      withDatabaseExcuse(request)(f.flatMap(t => sendSuccess(request, t)))

    message match
      case GetStatus =>
        player
          .status(cloudHost)
          .flatMap: status =>
            sendSuccess(request, StatusMessage(status))
      case GetTrack(id) =>
        uploader
          .upload(id, request)
          .handleError: t =>
            log.error(s"Upload failed for $request", t)
      case rt: RangedTrack =>
        uploader
          .rangedUpload(rt, request)
          .handleError: t =>
            log.error(s"Ranged upload failed for $request", t)
      case CancelStream(req) =>
        uploader.cancelSoon(req)
      case RootFolder =>
        lib.rootFolder
          .flatMap: folder =>
            sendSuccess(request, folder.toFull(cloudHost))
          .handleErrorWith: t =>
            log.error(s"Root folder failure.", t)
            sendFailure(request, FailReason("Library failure."))
      case GetFolder(id) =>
        lib
          .folder(id)
          .flatMap: maybeFolder =>
            maybeFolder
              .map: folder =>
                sendSuccess(request, folder.toFull(cloudHost))
              .getOrElse:
                val msg = s"Folder not found: '$id'."
                log.warn(msg)
                sendFailure(request, FailReason(msg))
          .handleErrorWith: t =>
            val msg = s"Library failure for folder '$id'."
            log.error(msg, t)
            sendFailure(request, FailReason(msg))
      case Search(term, limit) =>
        val ts = fullText
          .fullText(term, limit)
          .map: dataTracks =>
            dataTracks.map(t => TrackJson.toFull(t, cloudHost))
        databaseResponse(ts).void
      case PingAuth =>
        sendSuccess(request, JsonMessages.version)
      case PingMessage =>
        sendSuccess(request, PingEvent)
      case GetPopular(meta) =>
        databaseResponse:
          stats.mostPlayed(meta).map(PopularList.forEntries(meta, _, cloudHost))
      case GetRecent(meta) =>
        databaseResponse:
          stats.mostRecent(meta).map(RecentList.forEntries(meta, _, cloudHost))
      case GetPlaylists(user) =>
        databaseResponse:
          playlists.playlistsMeta(user).map(TrackJson.toFullPlaylistsMeta(_, cloudHost))
      case GetPlaylist(id, user) =>
        withDatabaseExcuse(request):
          playlists
            .playlistMeta(id, user)
            .flatMap: maybePlaylist =>
              maybePlaylist
                .map: playlist =>
                  sendSuccess(request, TrackJson.toFullMeta(playlist, cloudHost))
                .getOrElse:
                  sendFailure(request, FailReason(s"Playlist not found: '$id'."))
      case SavePlaylist(playlist, user) =>
        databaseResponse:
          playlists.saveOrUpdatePlaylistMeta(playlist, user)
      case DeletePlaylist(id, user) =>
        withDatabaseExcuse(request):
          playlists
            .delete(id, user)
            .flatMap: _ =>
              sendLogged(CloudResponse.ack(request))
      case GetAlarms =>
        deps.schedules
          .clockList(cloudHost)
          .flatMap: list =>
            sendLogged(CloudResponse.success(request, list))
          .recoverWith:
            case e: Exception =>
              val msg = "Unable to load schedules."
              log.error(msg, e)
              sendFailure(request, FailReason(msg))
      case AlarmEdit(payload) =>
        alarmHandler.handleCommand(payload) >>
          sendSuccess(request, Json.obj())
      case AlarmAdd(payload) =>
        alarmHandler.handleCommand(payload) >>
          sendSuccess(request, Json.obj())
      case Authenticate(user, pass) =>
        val authentication = deps.userManager
          .authenticate(user, pass)
          .recover:
            case t =>
              log.error(s"Database failure when authenticating '$user'.", t)
              false
        authentication.flatMap: isValid =>
          if isValid then sendSuccess(request, JsonMessages.version)
          else sendFailure(request, JsonMessages.invalidCredentials)
      case GetVersion =>
        sendSuccess(request, JsonMessages.version)
      case GetMeta(id) =>
        lib
          .meta(id)
          .flatMap: maybeTrack =>
            maybeTrack
              .map: track =>
                sendSuccess(request, TrackJson.toFull(track, cloudHost))
              .getOrElse:
                sendFailure(request, LibraryController.noTrackJson(id))
          .recoverWith:
            case e: Exception =>
              log.error(s"Unable to obtain meta of '$id'.", e)
              sendFailure(request, LibraryController.noTrackJson(id))
      case RegistrationEvent(_, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handlePlayerMessage(payload, user)
      case beamCommand: BeamCommand =>
        Rest
          .beam(beamCommand, lib, deps.http)
          .map: e =>
            e.fold(
              err => log.warn(s"Unable to beam. $err"),
              _ => log.info("Beaming completed successfully.")
            )
          .handleError(t => log.warn(s"Beaming failed.", t))
          .flatMap(_ => sendLogged(CloudResponse.ack(request)))

      case _ =>
        log.warn(s"Unknown request: '$message'.")
        sendFailure(request, FailReason(s"Unknown message in request '$request'."))

  private def withDatabaseExcuse[T](request: RequestID)(f: F[T]): F[Unit] =
    f.void.handleErrorWith: t =>
      log.error(s"Request $request error.", t)
      sendFailure(request, JsonMessages.databaseFailure)

  private def handleEvent(e: PimpMessage): F[Unit] =
    e match
      case RegisteredMessage(id) =>
        onRegistered(id)
      case RegistrationEvent(_, id) =>
        onRegistered(id)
      case PlaybackMessage(payload, user) =>
        handlePlayerMessage(payload, user)
      case PingMessage =>
        F.unit
      case PongMessage =>
        F.unit
      case other =>
        F.delay(log.warn(s"Unknown event: '$other'."))

  private def handlePlayerMessage(message: PlayerMessage, user: Username): F[Unit] =
    F.delay(handler.updateUser(user)) >>
      handler.fulfillMessage(message, RemoteInfo.cloud(user, cloudHost))

  private def sendSuccess[T: Encoder](request: RequestID, response: T) =
    sendLogged(CloudResponse.success(request, response))

  private def sendFailure(request: RequestID, reason: FailReason) =
    sendLogged(CloudResponse.failed(request, reason))

  private def sendLogged[T: Encoder](response: CloudResponse[T]): F[Unit] =
    val request = response.request
    Async[F].fromTry:
      send(response.asJson)
        .map(_ => log.debug(s"Responded to request $request with payload '$response'."))
        .recover:
          case t => log.error(s"Unable to respond to $request with payload '$response'.", t)

  private def handleError(errors: DecodingFailure, json: Json): F[Unit] =
    F.delay(log.warn(errorMessage(errors, json)))

  def errorMessage(errors: DecodingFailure, json: Json): String =
    s"JSON error: $errors. Message: $json"

  private def onRegistered(id: CloudID): F[Unit] =
    for
      _ <- registrationPromise.complete(Right(id))
      _ <- registrationsHub.publish1(id)
    yield
      log.info(s"Connected as '$username' to $uri.")
      ()

  override def onClose(): F[Unit] =
    failSocket(CloudSocket.connectionClosed) >> F.delay(
      log.info(s"Disconnected as '$username' from $uri.")
    )

  override def onError(e: Exception): F[Unit] =
    failSocket(e)

  override def close(): Unit =
    d.unsafeRunAndForget(failSocket(CloudSocket.manuallyClosed))
    uploader.close()
    super.close()

  private def failSocket(e: Exception): F[Unit] =
    registrationPromise.complete(Left(e)).void
