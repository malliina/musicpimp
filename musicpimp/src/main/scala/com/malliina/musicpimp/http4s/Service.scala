package com.malliina.musicpimp.http4s

import cats.effect.Async
import cats.effect.std.Dispatcher
import cats.implicits.{catsSyntaxApplicativeId, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
import cats.syntax.all.catsSyntaxApplicativeError
import ch.qos.logback.classic.Level
import com.malliina.audio.meta.{SongMeta, SongTags, StreamSource}
import com.malliina.file.FileUtilities
import com.malliina.html.UserFeedback
import com.malliina.http.Errors
import com.malliina.http.io.HttpClientF2
import com.malliina.http4s.{FormReadableT, QueryParsers}
import com.malliina.logback.fs2.DefaultFS2IOAppender
import com.malliina.musicpimp.audio.{MusicPlayer, PimpEnc, PlaybackMessageHandler, StatsPlayer, StreamedTrack, Track, TrackJson, TrackMeta}
import com.malliina.musicpimp.auth.{AuthBundle, AuthedRequest, Http4sAuth, JsonRequest, PimpAuthenticator, Proxies2, UserManager, UserPayload}
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.db.{DataTrack, DatabaseLibrary, FullText, Indexer}
import com.malliina.musicpimp.exception.{PimpException, UnauthorizedException}
import com.malliina.musicpimp.html.{AlarmContent, ChangeLogLevel, InField, LibraryContent, LoginContent, PimpHtml, UsersContent}
import com.malliina.musicpimp.http4s.Service.log
import com.malliina.musicpimp.js.SearchStrings
import com.malliina.musicpimp.json.{JsonMessages, JsonStrings}
import com.malliina.musicpimp.library.{FileLibrary, Library, LocalTrack, MusicFolder, PlaylistService, PlaylistSubmission, SavePlaylistBody, Settings, SimpleMessage}
import com.malliina.musicpimp.messaging.adm.AmazonDevices
import com.malliina.musicpimp.messaging.apns.APNSDevices
import com.malliina.musicpimp.messaging.gcm.GoogleDevices
import com.malliina.musicpimp.messaging.mpns.PushUrls
import com.malliina.musicpimp.messaging.{Adm, Apns, Gcm, Mpns, TokenInfo, TokenPlatform, Tokens}
import com.malliina.musicpimp.models.{CloudID, FailReason, FolderID, FrontLogEvent, FrontLogEvents, NewUser, PlaylistID, TrackID}
import com.malliina.musicpimp.scheduler.json.JsonHandler
import com.malliina.musicpimp.scheduler.{ClockPlaybackConf, ScheduledPlaybackService}
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularList, RecentList}
import com.malliina.musicpimp.{BuildInfo, BuildMeta}
import com.malliina.play.ContentRange
import com.malliina.play.auth.RememberMeCredentials
import com.malliina.play.controllers.AccountKeys
import com.malliina.play.http.FullUrls
import com.malliina.play.models.PasswordChange
import com.malliina.storage.StorageLong
import com.malliina.util.{AppLogger, Logging}
import com.malliina.values.{ErrorMessage, Password, UnixPath, Username}
import com.malliina.web.Utils
import controllers.musicpimp.Cloud.ToggleCloudId
import controllers.musicpimp.{Accounts, Cloud, RemoveToken, Rest, Search, SettingsController, Website}
import fs2.io.file.Files
import io.circe.syntax.EncoderOps
import io.circe.{Encoder, Json}
import org.http4s.headers.{`Content-Length`, `Content-Type`}
import org.http4s.multipart.Multipart
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.{Header, Headers, HttpRoutes, MediaType, Request, Response, StaticFile, Status}
import org.slf4j.{Logger, LoggerFactory}
import org.typelevel.ci.CIString

import java.net.{URLDecoder, UnknownHostException}
import java.nio.charset.StandardCharsets
import java.nio.file.{Files as JFiles, Paths as JPaths}
import javax.sound.sampled.AudioSystem

object Service:
  private val log = AppLogger(getClass)

  object UsernameVar extends Validating(Username.build)

  abstract class Validating[T](build: String => Either[ErrorMessage, T]):
    def unapply(str: String): Option[T] =
      build(str).toOption

class Service[F[_]: { Async, Files }](
  player: MusicPlayer[F],
  userManager: UserManager[F, Username, Password],
  auth: PimpAuthenticator[F],
  webAuth: AuthBundle[F, AuthedRequest[F]],
  cookies: Http4sAuth[F],
  val lib: DatabaseLibrary[F],
  val files: FileLibrary,
  playlists: PlaylistService[F],
  stats: PlaybackStats[F],
  statsPlayer: StatsPlayer[F],
  schedules: ScheduledPlaybackService[F],
  val indexer: Indexer[F],
  fullText: FullText[F],
  messageHandler: PlaybackMessageHandler[F],
  val clouds: Clouds[F],
  alarmHandler: JsonHandler[F],
  http: HttpClientF2[F],
  appender: DefaultFS2IOAppender[F],
  d: Dispatcher[F],
  html: PimpHtml
) extends AppImplicits[F]:
  val F = Async[F]
  private val accountKeys = AccountKeys
  val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      seeOther(Reverse.folders.base)
    case req @ GET -> Root / "ping" =>
      ok(SimpleMessage("pong"))
    case req @ GET -> Root / "pingAuth" =>
      authed(req): user =>
        ok(BuildMeta.default)
    case GET -> Root / "info" =>
      ok(Json.obj("git" -> BuildInfo.gitHash.asJson))
    case req @ GET -> Root / "login" =>
      userManager.isDefaultCredentials.flatMap: isDefault =>
        val motd =
          if isDefault then
            Option(
              Accounts.defaultCredentialsMessage(userManager.defaultUser, userManager.defaultPass)
            )
          else None
        ok(html.login(LoginContent(accountKeys, motd, None, req.userFeedback()))).clearFeedback
    case req @ GET -> Root / "logout" =>
      seeOther(reverse.login)
        .map(res => cookies.clearSession(res))
        .withFeedbackMessage(Accounts.logoutMessage)
    case req @ POST -> Root / "authenticate" =>
      req
        .attemptAs[RememberMeCredentials]
        .foldF(
          formErrors =>
            log.warn(s"Authentication failed: ${formErrors.message}")
            seeOther(Reverse.login).withFeedback(UserFeedback.error("Form error."))
          ,
          credentials =>
            val username = credentials.username
            auth
              .authenticate(username, credentials.password)
              .flatMap: isValid =>
                if isValid then
                  log.info(s"Authentication succeeded for user '$username'.")
                  val intendedUrl = cookies.readIntendedUri(req).getOrElse(reverse.folders.base)
                  seeOther(intendedUrl).map: res =>
                    cookies.withUser(UserPayload(username), Proxies2.isSecure(req), res)
                else
                  log.warn(s"Invalid form authentication for user '$username'.")
                  val formFeedback = UserFeedback.error("Incorrect username or password.")
                  val content = LoginContent(
                    accountKeys,
                    None,
                    Option(formFeedback),
                    req.userFeedback()
                  )
                  badRequestEntity(html.login(content)).clearFeedback
        )
    case req @ POST -> Root / "changePassword" =>
      authed(req): user =>
        req
          .attemptAs[PasswordChange]
          .foldF(
            formErrors =>
              log.warn(s"Authentication failed: ${formErrors.message}")
              badRequestEntity(html.account(user.username, req.userFeedback()))
            ,
            pc =>
              auth
                .authenticate(user.username, pc.oldPass)
                .flatMap: isValid =>
                  if isValid then
                    userManager
                      .updatePassword(user.username, pc.newPass)
                      .flatMap: _ =>
                        log.info(s"Password changed for user '$user'.")
                        seeOther(reverse.account)
                          .withFeedbackMessage(Accounts.passwordChangedMessage)
                  else
                    badRequestEntity(
                      html.account(
                        user.username,
                        Option(UserFeedback.error(Accounts.incorrectPasswordMessage))
                      )
                    )
          )
    case req @ POST -> Root / "addUser" =>
      authed(req): user =>
        req
          .attemptAs[NewUser]
          .foldF(
            formErrors =>
              log.warn(
                s"Unable to add user '$user' form: $formErrors"
              )
              userManager.users.flatMap: users =>
                badRequestEntity(usersPage(users, user.username, req))
            ,
            ok =>
              userManager
                .addUser(ok.username, ok.pass)
                .flatMap: addError =>
                  val userFeedback = addError
                    .map(e => UserFeedback.error(s"User '${e.user}' already exists."))
                    .getOrElse(UserFeedback.success(s"Created user '${ok.username}'."))
                  seeOther(reverse.users.base).withFeedback(userFeedback)
          )
    case req @ GET -> Root / "folders" =>
      authed(req): user =>
        lib.rootFolder.flatMap: root =>
          given Encoder[MusicFolder] = MusicFolder.writer(req)
          pimpResult(req)(
            html = ok(html.flexLibrary(root, user.user.username)),
            json = ok(root)
          )
    case req @ GET -> Root / "folders" / FolderID(id) =>
      authed(req): user =>
        val folderId = PimpEnc.folder(id)
        given Encoder[MusicFolder] = MusicFolder.writer(req)
        lib
          .folder(folderId)
          .flatMap: maybeFolder =>
            maybeFolder
              .map: items =>
                respond(req)(
                  html = html.flexLibrary(items, user.username),
                  json = items
                )
              .getOrElse:
                notFound(s"Folder not found: $id")
    case req @ GET -> Root / "downloads" / TrackID(id) =>
      download(id, req)
    case req @ GET -> Root / "tracks" / "folders" =>
      tracksIn(Library.RootId, req)
    case req @ GET -> Root / "tracks" / "folders" / FolderID(id) =>
      tracksIn(id, req)
    case req @ GET -> Root / "tracks" / "meta" / TrackID(id) =>
      authed(req): user =>
        lib
          .track(id)
          .flatMap: maybeTrack =>
            maybeTrack
              .map: t =>
                ok(TrackJson.toFull(t, FullUrls.hostOnly2(req)))
              .getOrElse:
                badRequest(s"Track not found: $id")
    case req @ GET -> Root / "tracks" / TrackID(id) =>
      download(id, req)
    case req @ GET -> Root / "player" =>
      authed(req): user =>
        val hasAudioDevice = AudioSystem.getMixerInfo.nonEmpty
        val feedback: Option[String] =
          if !hasAudioDevice then
            Some("Unable to access audio hardware. Playback on this machine is likely to fail.")
          else player.errorOpt.map(Website.errorMsg)
        val userFeedback = feedback.map(UserFeedback.error)
        ok(html.basePlayer(userFeedback, user.username))
    case req @ GET -> Root / "player" / "recent" =>
      metaAction(req): meta =>
        stats
          .mostRecent(meta)
          .flatMap: entries =>
            val list = RecentList.forEntries(meta, entries, FullUrls.hostOnly2(req))
            pimpResult(req)(
              html = ok(html.mostRecent(list)),
              json = ok(list)
            )
    case req @ GET -> Root / "player" / "popular" =>
      metaAction(req): meta =>
        stats
          .mostPlayed(meta)
          .flatMap: entries =>
            val list = PopularList.forEntries(meta, entries, FullUrls.hostOnly2(req))
            pimpResult(req)(
              html = ok(html.mostPopular(list)),
              json = ok(list)
            )
    case req @ GET -> Root / "playlists" =>
      playlistAction(req): user =>
        playlists
          .playlistsMeta(user.username)
          .flatMap: lists =>
            pimpResult(req)(
              html = ok(html.playlists(lists.playlists, user.username)),
              json = ok(TrackJson.toFullPlaylistsMeta(lists, FullUrls.hostOnly2(req)))
            )
    case req @ GET -> Root / "playlists" / PlaylistID(id) =>
      playlistAction(req): user =>
        playlists
          .playlistMeta(id, user.username)
          .flatMap: result =>
            result
              .map: playlist =>
                pimpResult(req)(
                  html = ok(html.playlist(playlist.playlist, user.username)),
                  json = ok(TrackJson.toFullMeta(playlist, FullUrls.hostOnly2(req)))
                )
              .getOrElse:
                notFound(s"Playlist not found: $id")
    case req @ POST -> Root / "playlists" =>
      playlistAction(req): user =>
        req
          .decodeJson[SavePlaylistBody]
          .flatMap: save =>
            playlists
              .saveOrUpdatePlaylistMeta(save.playlist, user.username)
              .flatMap(meta => accepted(meta))
    case req @ POST -> Root / "playlists" / "delete" / PlaylistID(id) =>
      playlistAction(req): user =>
        playlists.delete(id, user.username).flatMap(_ => accepted(SimpleMessage("Deleted.")))
    case req @ POST -> Root / "playlists" / "edit" =>
      playlistAction(req): user =>
        ok(SimpleMessage("This endpoint does nothing."))
    case req @ POST -> Root / "playlists" / "handle" =>
      playlistAction(req): user =>
        req
          .attemptAs[PlaylistSubmission]
          .foldF(
            err =>
              playlists
                .playlistsMeta(user.username)
                .flatMap(pls => badRequestEntity(html.playlists(pls.playlists, user.username))),
            ok => seeOther(reverse.playlists.base)
          )
    case req @ GET -> Root / "settings" =>
      settings(req)
    case req @ GET -> Root / "manage" =>
      settings(req)
    case req @ GET -> Root / "about" =>
      authed(req): user =>
        ok(html.aboutBase(user.username))
    case req @ GET -> Root / "account" =>
      authed(req): user =>
        ok(html.account(user.username, req.userFeedback())).clearFeedback
    case req @ GET -> Root / "users" =>
      authed(req): user =>
        userManager.users.flatMap: users =>
          ok(usersPage(users, user.username, req)).clearFeedback
            .map(_.removeCookie(Accounts.UsersFeedback))
    case req @ POST -> Root / "users" / "delete" / Service.UsernameVar(targetUser) =>
      authed(req): user =>
        val redir = seeOther(reverse.users.base)
        if user.username != targetUser then
          userManager
            .deleteUser(targetUser)
            .flatMap: _ =>
              redir
                .withFeedback(Map(Accounts.UsersFeedback -> s"Deleted user '$targetUser'."))
        else
          redir.withFeedback(
            Map(
              Accounts.UsersFeedback -> Accounts.cannotDeleteYourself,
              UserFeedback.Success -> UserFeedback.No
            )
          )
    case req @ POST -> Root / "rootfolders" =>
      authed(req): user =>
        req
          .attemptAs[NewFolder]
          .foldF(
            err =>
              log.warn(s"Errors: $err.")
              badRequest(err.message)
            ,
            ok =>
              Settings.add(JPaths.get(ok.path))
              files.reloadFolders()
              seeOther(reverse.settings).withFeedbackMessage(s"Added folder '${ok.path}'.")
          )
    case req @ POST -> Root / "rootfolders" / "delete" / id =>
      authed(req): user =>
        val decoded = URLDecoder.decode(id, StandardCharsets.UTF_8)
        val path = JPaths.get(decoded)
        Settings.delete(path)
        files.reloadFolders()
        seeOther(reverse.settings).withFeedbackMessage(s"Removed folder '$id'.")
    case req @ GET -> Root / "alarms" =>
      authed(req): user =>
        schedules
          .clockList(FullUrls.hostOnly2(req))
          .flatMap: fcps =>
            pimpResult(req)(
              html = ok(html.alarms(fcps, user.username)),
              json = ok(fcps)
            )
    case req @ POST -> Root / "alarms" =>
      authed(req): user =>
        req
          .attemptAs[Json]
          .foldF(
            err => badRequest("Not JSON."),
            json =>
              val remoteAddress = Proxies2.realAddress(req.headers)
              log.debug(s"User '${user.username}' from '$remoteAddress' said '$json'.")
              alarmHandler
                .handle(json)
                .fold(
                  errors => badRequest(s"Invalid JSON '$json'. Errors '$errors'."),
                  _ => ok(SimpleMessage("Handled."))
                )
          )
    case req @ GET -> Root / "alarms" / "editor" =>
      authed(req): user =>
        ok(html.alarmEditor(AlarmContent(None, None, user.username)))
    case req @ GET -> Root / "alarms" / "editor" / id =>
      authed(req): user =>
        ok(
          html.alarmEditor(AlarmContent(schedules.find(id), req.userFeedback(), user.username))
        ).clearFeedback
    case req @ POST -> Root / "alarms" / "editor" / "add" =>
      authed(req): user =>
        val username = user.username
        req
          .attemptAs[ClockPlaybackConf]
          .foldF(
            err => badRequestEntity(html.alarmEditor(AlarmContent(None, None, username))),
            form =>
              val task = F.delay:
                schedules.save(form)
                log.info(s"User '$username' saved alarm '$form'.")
              task.flatMap: _ =>
                val content =
                  AlarmContent(Option(form), Option(UserFeedback.success("Saved.")), username)
                ok(html.alarmEditor(content))
          )
    case req @ GET -> Root / "tracks" =>
      authed(req): user =>
        val ts = files.tracksRecursive.map: t =>
          TrackJson.toFull(t, FullUrls.hostOnly2(req))
        ok(ts)
    case req @ GET -> Root / "pathsOnly" =>
      authed(req): user =>
        ok(files.songPathsRecursive.toList)
    case req @ GET -> Root / "manage" / "push" / "tokens" =>
      def ts =
        APNSDevices.get().map(d => TokenInfo(d.id, Apns)) ++
          PushUrls.get().map(p => TokenInfo(p.url, Mpns)) ++
          GoogleDevices.get().map(g => TokenInfo(g.id, Gcm)) ++
          AmazonDevices.get().map(a => TokenInfo(a.id, Adm))
      authed(req): user =>
        pimpResult(req)(
          html = ok(html.tokens(ts, user.username, req.feedbackAs[UserFeedback])),
          json = ok(Tokens(ts))
        )
    case req @ GET -> Root / "manage" / "push" / "tokens" / "delete" =>
      authed(req): user =>
        req
          .attemptAs[RemoveToken]
          .foldF(
            err => badRequestEntity("Bad request."),
            rt =>
              val token = rt.token
              TokenPlatform
                .build(rt.platform)
                .map:
                  case Apns => APNSDevices.removeWhere(_.id.token == token)
                  case Mpns => PushUrls.removeURL(token)
                  case Gcm  => GoogleDevices.removeWhere(_.id.token == token)
                  case Adm  => AmazonDevices.removeWhere(_.id.token == token)
                .map: _ =>
                  seeOther(reverse.manage.push.tokens)
                    .withFeedback(UserFeedback.success("Removed."))
                .getOrElse:
                  badRequestEntity("Unknown platform.")
          )
    case req @ GET -> Root / "search" =>
      authed(req): user =>
        val query = req.uri.query
        val parsed = for
          term <- QueryParsers.parseOptE[String](query, SearchStrings.TermKey)
          limit <- QueryParsers.parseOptE[Int](query, SearchStrings.LimitKey)
        yield (term, limit)
        parsed.fold(
          errors =>
            log.warn(s"Failed to parse query. ${errors.message}")
            badRequest("Invalid query parameters.")
          ,
          (term, limit) =>
            val results = term
              .map: t =>
                fullText.fullText(t, limit.getOrElse(Search.DefaultLimit))
              .getOrElse:
                F.pure(List.empty[DataTrack])
            results.flatMap: tracks =>
              respond(req)(
                html = html.search(term, tracks, user.username),
                json = tracks.map(dt => TrackJson.toFull(dt, FullUrls.hostOnly2(req)))
              )
        )
    case req @ POST -> Root / "search" / "refresh" =>
      indexer
        .submitIndexAndSave()
        .flatMap: _ =>
          ok(SimpleMessage("Refreshing..."))
    case req @ GET -> Root / "playback" =>
      authed(req): user =>
        val host = FullUrls.hostOnly2(req)
        response(req)(
          html = F.pure(Response(noContent)),
          json17 = player.status17(host).flatMap(s => ok(s)),
          latest = player.status(host).flatMap(s => ok(s))
        )
    case req @ POST -> Root / "playback" =>
      postPlaylistPlayback(req)
    case req @ POST -> Root / "playback" / "uploads" =>
      uploadedAction(req): track =>
        player.setPlaylistAndPlay(track)
    case req @ POST -> Root / "playlist" =>
      postPlaylistPlayback(req)
    case req @ POST -> Root / "playlist" / "uploads" =>
      uploadedAction(req): track =>
        player.playlist.add(track)
    case req @ POST -> Root / "playback" / "stream" =>
      authed(req): user =>
        req
          .decodeJson[BeamCommand]
          .flatMap: cmd =>
            Rest
              .beam(cmd, lib, http)
              .flatMap: e =>
                e.fold(
                  err => badRequest(err.message),
                  res =>
                    // relays MusicBeamer's response to the client
                    val statusCode = res.code
                    log.info(s"Completed track upload, relaying response: $statusCode")
                    Status
                      .fromInt(statusCode)
                      .fold(
                        fail => badGateway(s"Unsupported status code '$statusCode'."),
                        status =>
                          if statusCode >= 200 && statusCode < 300 then
                            Response[F](status)
                              .withEntity[Json](JsonMessages.thanks)
                              .pure
                          else Response[F](status).pure
                      )
                )
              .handleErrorWith:
                case uhe: UnknownHostException =>
                  notFound(s"Unable to find MusicBeamer endpoint. ${uhe.getMessage}")
                case e: Exception =>
                  val msg = "Stream failure."
                  log.error(msg, e)
                  internal(msg)
    case req @ POST -> Root / "playback" / "server" =>
      import io.circe.parser.decode
      authed(req): user =>
        val metaOrError =
          req.headers
            .get(CIString(JsonStrings.TrackHeader))
            .map(_.head.value)
            .toRight("No Track header is defined.")
            .flatMap(v => decode[Track](v).left.map(err => s"Invalid JSON: $err"))
        metaOrError.fold(
          err => badRequest(err),
          meta =>
            statsPlayer.updateUser(user.username)
            lib
              .meta(meta.id)
              .flatMap: opt =>
                opt
                  .map: track =>
                    player.setPlaylistAndPlay(track)
                    log.info(s"Play local file of: ${track.id}")
                    ok(SimpleMessage("Playing local file."))
                  .getOrElse:
                    val relative = meta.path
                    val fileOpt = files
                      .findAbsoluteNew(relative)
                      .filter(Rest.canWriteNewFile)
                      .orElse(
                        Option(FileUtilities.tempDir.resolve(meta.relativePath))
                          .filter(Rest.canWriteNewFile)
                      )
                    val msg =
                      fileOpt.fold(s"Streaming: $relative")(path =>
                        s"Streaming: $relative and saving to: $path"
                      )
                    log.info(msg)
                    // TODO limit to 1024.megs
                    req
                      .as[Multipart[F]]
                      .flatMap: mp =>
                        mp.parts
                          .flatMap(p => p.filename.map(n => (p, n)))
                          .headOption
                          .map: (part, name) =>
                            for
                              inStream <- part.body
                                .through(fs2.io.toInputStream[F])
                                .compile
                                .toList
                                .map(_.head)
                              track = StreamedTrack.fromTrack(meta, inStream)
                              _ <- player.setPlaylistAndPlay(track)
                              res <- ok(SimpleMessage("Thanks."))
                            yield res
                          .getOrElse:
                            badRequest("No file to stream.")
        )
    case req @ GET -> Root / "cloud" =>
      authed(req): user =>
        val feedback = req.feedbackAs[UserFeedback]
        clouds.registration
          .map: cloudId =>
            html.cloud(Option(cloudId), feedback, user.username)
          .handleError: t =>
            val errorFeedback = feedback.getOrElse(UserFeedback.error(t.getMessage))
            html.cloud(None, Option(errorFeedback), user.username)
          .flatMap: tags =>
            ok(tags)
    case req @ POST -> Root / "cloud" =>
      authed(req): user =>
        val redir = seeOther(reverse.cloud)
        req
          .attemptAs[ToggleCloudId]
          .foldF(
            err =>
              val feedback = UserFeedback.error(err.message)
              badRequestEntity(html.cloud(None, Option(feedback), user.username))
            ,
            desiredId =>
              if clouds.isConnected then
                clouds.disconnectAndForget("Disconnected by request.") >> redir
              else
                val maybeId = desiredId.id.filter(_.nonEmpty).map(CloudID.apply)
                clouds
                  .connect(maybeId)
                  .flatMap(_ => redir)
                  .handleErrorWith: t =>
                    redir.withFeedback(UserFeedback.error(Cloud.errorMessage(t, clouds.uri)))
          )
    case req @ GET -> Root / "logs" =>
      authed(req): user =>
        ok(
          html.logs(
            InField.id(ChangeLogLevel.LevelKey),
            Logging.levels,
            Logging.level,
            user.username,
            req.userFeedback()
          )
        ).clearFeedback
    case req @ POST -> Root / "logs" / "levels" =>
      authed(req): user =>
        req
          .attemptAs[ChangeLogLevel]
          .foldF(
            err =>
              badRequestEntity(
                html.logs(
                  InField.id(ChangeLogLevel.LevelKey),
                  Logging.levels,
                  Logging.level,
                  user.username,
                  Option(UserFeedback.error(err.message))
                )
              ),
            form =>
              Logging.level = form.level
              log.warn(s"Changed log level to ${form.level}")
              seeOther(reverse.logs.base)
          )
    case req @ POST -> Root / "logs" =>
      authed(req): user =>
        req
          .decodeJson[FrontLogEvents]
          .flatMap: es =>
            val task = F.delay:
              es.events.foreach(handleLog)
            task >> accepted(SimpleMessage("Thanks."))

  def handleLog(event: FrontLogEvent): Unit =
    logFunc(LoggerFactory.getLogger(event.module), Level.toLevel(event.level))(event.message)

  def logFunc(logger: Logger, level: Level): String => Unit =
    if level == Level.DEBUG then logger.debug
    else if level == Level.INFO then logger.info
    else if level == Level.WARN then logger.warn
    else if level == Level.ERROR then logger.error
    else logger.trace

  def sockets(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    val psb = PlayerSocketBuilder(player, messageHandler)
    val csb = CloudSocketBuilder(clouds)
    val ssb = SearchSocketBuilder(indexer)
    val lsb = LogSocketBuilder[F](appender)
    HttpRoutes.of[F]:
      case req @ GET -> Root / "ws" / "playback" =>
        authed(req): user =>
          psb.playback(user, builder)
      case req @ GET -> Root / "ws" / "playback2" =>
        authed(req): user =>
          psb.playback(user, builder)
      case req @ GET -> Root / "ws" / "cloud" =>
        authed(req): user =>
          csb.flow(user, builder)
      case req @ GET -> Root / "ws" / "logs" =>
        authed(req): user =>
          lsb.build(user, builder)
      case req @ GET -> Root / "search" / "ws" =>
        authed(req): user =>
          log.info(s"Opening logs socket for '${user.username}'...")
          ssb.socket(user, builder)

  private def download(id: TrackID, req: Request[F]) =
    authed(req): user =>
      lib
        .findFile(id)
        .flatMap: maybeTrack =>
          maybeTrack
            .map: path =>
              val size = JFiles.size(path).bytes
              val rangeOpt = req.headers
                .get(CIString("Range"))
                .flatMap(h => ContentRange.fromHeader(h.head.value, size).toOption)
              val fs2Path = fs2.io.file.Path.fromNioPath(path)
              rangeOpt
                .map: range =>
                  val r = Response(
                    Status.PartialContent,
                    headers = Headers(
                      `Content-Length`.fromLong(range.size.bytes).toOption,
                      nameToContentType(fs2Path.fileName.toString),
                      Header.Raw(CIString("Content-Range"), range.contentRange)
                    ),
                    body = Files[F].readRange(
                      fs2Path,
                      StaticFile.DefaultBufferSize,
                      range.start.toLong,
                      range.endExclusive.toLong
                    )
                  )
                  F.pure(r)
                .getOrElse:
                  StaticFile
                    .fromPath(fs2Path, Option(req))
                    .map: res =>
                      res.withHeaders(Header.Raw(CIString("Accept-Ranges"), ContentRange.BYTES))
                    .getOrElseF(notFound(s"Track not found: $id"))
            .getOrElse:
              notFound(s"Track not found: $id")

  private def tracksIn(id: FolderID, req: Request[F]) =
    authed(req): user =>
      val folderId = PimpEnc.folder(id)
      given Encoder[TrackMeta] = TrackJson.writer(FullUrls.hostOnly2(req))
      given Encoder[List[TrackMeta]] = Encoder.encodeList[TrackMeta]
      lib
        .tracksIn(folderId)
        .flatMap: maybeTracks =>
          maybeTracks
            .map: tracks =>
              ok(tracks)
            .getOrElse:
              notFound(s"Folder not found: $id")

  private def nameToContentType(name: String): Option[`Content-Type`] =
    name.lastIndexOf('.') match
      case -1 => None
      case i  => MediaType.forExtension(name.substring(i + 1)).map(`Content-Type`(_))

  private def postPlaylistPlayback(req: Request[F]) =
    authed(req): user =>
      req
        .attemptAs[Json]
        .foldF(
          err =>
            log.warn(s"Not JSON. Got error: $err.")
            badRequest("Not JSON.")
          ,
          json =>
            F.delay(messageHandler.onJson(JsonRequest[F](user.user, req, json, None)))
              .flatMap: _ =>
                Accepted(SimpleMessage("Thanks."))
              .handleErrorWith:
                case iae: IllegalArgumentException =>
                  log.error("Illegal argument", iae)
                  badRequest(iae.getMessage)
                case t: Throwable =>
                  log.error("Unable to execute action", t)
                  internalGeneric
        )

  private def uploadedAction(req: Request[F])(action: LocalTrack => F[Unit]): F[Response[F]] =
    authed(req): user =>
      req
        .as[Multipart[F]]
        .flatMap: mp =>
          mp.parts
            .flatMap(p => p.filename.map(n => (p, n)))
            .headOption
            .map: (part, filename) =>
              val copy = mp
                .textF("path")
                .flatMap: pathOpt =>
                  val absolutePath = pathOpt
                    .map: pathStr =>
                      val path = JPaths.get(pathStr)
                      val libraryPath = files
                        .suggestAbsolute(path)
                        .filter(!JFiles.exists(_))
                      val absolutePath = libraryPath.getOrElse(
                        FileUtilities.tempDir.resolve(s"upload-${Utils.randomString().take(4)}")
                      )
                      Option(absolutePath.getParent).map(JFiles.createDirectories(_))
                      absolutePath
                    .getOrElse:
                      FileUtilities.tempDir.resolve(s"upload-${Utils.randomString().take(4)}")
                  val fs2Path = fs2.io.file.Path.fromNioPath(absolutePath)
                  part.body
                    .through(Files[F].writeAll(fs2Path))
                    .compile
                    .drain
                    .as(absolutePath)
              def trackFromFile(file: java.nio.file.Path) =
                for
                  title <- mp.textF("title")
                  album <- mp.textOrEmpty("album")
                  artist <- mp.textOrEmpty("artist")
                yield
                  val meta = SongMeta(
                    StreamSource.fromFile(file),
                    SongTags(title.getOrElse(file.getFileName.toString), album, artist)
                  )
                  LocalTrack(Library.trackId(file), UnixPath(file), meta)
              def readMetadata(file: java.nio.file.Path) =
                val trackId = Library.trackId(file)
                lib
                  .meta(trackId)
                  .flatMap(opt => opt.map(lt => F.pure(lt)).getOrElse(trackFromFile(file)))
              for
                file <- copy
                meta <- readMetadata(file)
                _ <- F.delay(log.info(s"User ${user.username} uploaded ${meta.meta.media.size}."))
                _ <- action(meta)
                _ = statsPlayer.updateUser(user.username)
                res <- accepted(SimpleMessage("Thanks."))
              yield res
            .getOrElse:
              badRequest("File missing")

  extension (mp: Multipart[F])
    def text(name: String): Option[F[String]] =
      mp.parts.find(_.name.contains(name)).map(_.bodyText.compile.string)
    def textF(name: String): F[Option[String]] =
      text(name).map(fs => fs.map(s => Option(s))).getOrElse(F.pure(None))
    def textOrEmpty(name: String): F[String] = text(name).getOrElse(F.pure(""))

  private def usersPage(users: Seq[Username], username: Username, req: Request[?]) =
    val feedback = req.userFeedback()
    val listFeedback = req.userFeedback(Accounts.UsersFeedback)
    val content = UsersContent(users, username, listFeedback, feedback)
    html.users(content)

  import cats.syntax.all.toSemigroupKOps

  def allRoutes(builder: WebSocketBuilder2[F]): HttpRoutes[F] = routes.combineK(sockets(builder))

  case class NewFolder(path: String)

  object NewFolder:
    given FormReadableT[NewFolder] = FormReadableT.reader.emap: form =>
      form
        .read[String](SettingsController.Path)
        .filterOrElse(
          s => s.nonEmpty && SettingsController.validateDirectory(s),
          Errors.single("Invalid directory.")
        )
        .map(s => NewFolder(s))

  private def settings(req: Request[F]) =
    authed(req): user =>
      val fb = req.userFeedback()
      val content = LibraryContent(
        Settings.readFolders,
        SettingsController.folderPlaceHolder,
        user.username,
        fb
      )
      ok(html.musicFolders(content)).clearFeedback

  private def playlistAction(req: Request[F])(code: AuthedRequest[F] => F[Response[F]]) =
    authed(req)(code).handleErrorWith(playlistsErrorHandler)

  private def metaAction(req: Request[F])(code: DataRequest => F[Response[F]]) =
    authed(req): user =>
      DataRequest
        .fromReq(user.user.username, req)
        .fold(err => badRequestWithErrors(err), ok => code(ok))

  private def authed(req: Request[F])(code: AuthedRequest[F] => F[Response[F]]) =
    webAuth.authenticator
      .authenticate(req)
      .flatMap: outcome =>
        outcome.fold(failure => webAuth.onUnauthorized(failure), user => code(user))

  extension (req: Request[?])
    def userFeedback(cookieName: String = feedbackCookieName) =
      req.cookies
        .find(_.name == cookieName)
        .map(_.content)
        .flatMap(f => io.circe.parser.decode[UserFeedback](f).toOption)

  private def playlistsErrorHandler: PartialFunction[Throwable, F[Response[F]]] =
    case ue: UnauthorizedException =>
      log.error(s"Unauthorized", ue)
      unauthorizedNoCache(FailReason("Access denied"))
    case pe: PimpException =>
      log.error(s"Pimp error", pe)
      internalGeneric
    case t: Throwable =>
      log.error(s"Server error", t)
      internalGeneric
