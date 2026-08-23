package com.malliina.pimpcloud.http4s

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.malliina.http.Errors
import com.malliina.musicpimp.audio.{Directory, PimpEnc, Track}
import com.malliina.musicpimp.auth.{Http4sAuth, Http4sAuthFailure, Proxies2, UserPayload}
import com.malliina.musicpimp.cloud.Search
import com.malliina.musicpimp.json.PimpStrings
import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID, Version, WrappedID, WrappedLong}
import com.malliina.musicpimp.stats.ItemLimits
import com.malliina.pimpcloud.auth.{CloudAuthentication, CloudCredentials}
import com.malliina.pimpcloud.html.CloudTags
import com.malliina.pimpcloud.http4s.AuthProvider.Google
import com.malliina.pimpcloud.http4s.Phones2.{name, trackHeaders}
import com.malliina.pimpcloud.http4s.Service.log
import com.malliina.pimpcloud.json.JsonStrings.{AlarmsAdd, AlarmsEdit, AlarmsKey, FolderKey, PlaylistDelete, PlaylistGet, PlaylistSave, PlaylistsGet, Popular, Recent, RootFolderKey, SearchKey, StatusKey}
import com.malliina.pimpcloud.ws.PhoneConnection
import com.malliina.pimpcloud.{BuildMeta, SharedStrings}
import com.malliina.play.ContentRange
import com.malliina.storage.{StorageInt, StorageLong}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import fs2.io.file.Files
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Encoder, Json}
import org.http4s.headers.Range.SubRange
import org.http4s.headers.{Range, `Content-Range`, `Content-Type`, `User-Agent`}
import org.http4s.multipart.Part
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.{EntityDecoder, HttpRoutes, MediaType, Request, Response}

object Service:
  private val log = AppLogger(getClass)

class Service[F[_]: { Async, Files }](
  sockets: Sockets[F],
  html: CloudTags,
  auth: CloudAuthentication[F],
  cookies: Http4sAuth[F],
  google: GoogleAuth[F]
) extends CloudImplicits[F]:
  private val F = Async[F]
  private val reverse = Reverse

  val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root / "health" =>
      ok(BuildMeta.default)
    case req @ GET -> Root / "ping" =>
      proxied(req): phone =>
        phone
          .request(SharedStrings.Ping)
          .flatMap: json =>
            ok(json)
    case req @ GET -> Root / "pingauth" =>
      proxiedJson[Json, Version](req, PimpStrings.VersionKey, Json.obj()): v =>
        ok(v)
    case req @ (GET -> Root | GET -> Root / "folders") =>
      proxiedFolder[Json](req, RootFolderKey, Json.obj())
    case req @ GET -> Root / "folders" / FolderID(id) =>
      proxiedFolder[WrappedID](req, FolderKey, WrappedID.forId(PimpEnc.folder(id)))
    case req @ GET -> Root / "playback" =>
      proxiedCommand(req, StatusKey)
    case req @ POST -> Root / "playback" / "stream" =>
      ok(Json.obj("message" -> "Not supported yet.".asJson))
    case req @ GET -> Root / "player" / "recent" =>
      paginated(req, Recent)
    case req @ GET -> Root / "player" / "popular" =>
      paginated(req, Popular)
    case req @ HEAD -> Root / ("tracks" | "downloads") / TrackID(id) =>
      withTrack(req, id): (_, track) =>
        val r = Response[F](Ok)
          .withContentType(`Content-Type`(MediaType.audio.mpeg))
          .putHeaders(trackHeaders(Phones2.name(track, id), track.size)*)
        F.pure(r)
    case req @ GET -> Root / ("tracks" | "downloads") / TrackID(id) =>
      withTrack(req, id): (phone, track) =>
        val userAgent = req.headers
          .get[`User-Agent`]
          .map(v => `User-Agent`.headerInstance.value(v))
          .getOrElse("undefined")
        log.info(
          s"Serving track '${track.title}' at '${track.path}' with ID '$id' to user agent '$userAgent'."
        )
        val rangeOpt = req.headers
          .get[Range]
          .flatMap(v => ContentRange.fromHeader(Range.headerInstance.value(v), track.size).toOption)
        val range = rangeOpt.getOrElse(ContentRange.all(track.size))
        phone.server
          .requestTrack(track, range, req)
          .map: res =>
            rangeOpt
              .map: r =>
                res.putHeaders(
                  `Content-Range`(
                    SubRange(range.start, range.endInclusive),
                    Option(track.size.bytes)
                  )
                )
              .getOrElse:
                res.putHeaders(trackHeaders(name(track, id), track.size)*)
    case req @ GET -> Root / "search" =>
      Search(req.uri.query).fold(
        err => badRequestWithErrors(err),
        s => proxiedFolder(req, SearchKey, s)
      )
    case req @ GET -> Root / "alarms" =>
      proxiedCommand(req, AlarmsKey)
    case req @ POST -> Root / "alarms" =>
      proxiedBody(req, AlarmsEdit)
    case req @ POST -> Root / "alarms" / "editor" / "add" =>
      proxiedBody(req, AlarmsAdd)
    case req @ GET -> Root / "playlists" =>
      proxiedCommand(req, PlaylistsGet)
    case req @ POST -> Root / "playlists" =>
      proxiedBody(req, PlaylistSave)
    case req @ GET -> Root / "playlists" / PlaylistID(id) =>
      proxiedBasic(req, PlaylistGet, WrappedLong(id.id))(json => ok(json))
    case req @ POST -> Root / "playlists" / "delete" / PlaylistID(id) =>
      proxiedBasic(req, PlaylistDelete, WrappedLong(id.id))(json => ok(json))
    case req @ POST -> Root / "proxied" / cmd =>
      proxiedBody(req, cmd)
    case req @ POST -> Root / "track" =>
      auth.server
        .authenticate(req)
        .flatMap: outcome =>
          outcome
            .map: server =>
              server.stream
                .map: stream =>
                  log.info(s"Processing ${server.request}...")
                  EntityDecoder
                    .mixedMultipartResource[F]()
                    .use: decoder =>
                      val maxSize = server.socket.fileTransfers.maxUploadSize
                      log.info(s"Streaming at most $maxSize for '${server.request}'.")
                      req.decodeWith(decoder, true): parts =>
                        val consume = fs2.Stream
                          .emits[F, Part[F]](parts.parts.filter(_.filename.isDefined))
                          .mapAsync(1): filePart =>
                            filePart.body
                              .chunkN(4096, allowFewer = true)
                              .evalMapAccumulate(0.bytes): (acc, chunk) =>
                                stream
                                  .send(chunk.toList)
                                  .map: b =>
                                    ((acc.bytes + chunk.size).bytes, b)
                              .compile
                              .last
                              .map(_.getOrElse((0.bytes, false)))
                        consume.compile.toList.flatMap: results =>
                          for
                            _ <- stream.close
                            removal <- server.cleanup(true)
                            result <-
                              val streamedSize =
                                results.foldLeft(0L)((acc, part) => acc + part._1.bytes).bytes
                              val fileCount = results.size
                              val fileDesc = if fileCount > 1 then "files" else "file"
                              log.info(
                                s"Streamed $streamedSize in $fileCount $fileDesc for '${server.request}'."
                              )
                              ok(Json.obj("message" -> "ok".asJson))
                          yield result
                .getOrElse:
                  notFound(s"Request not found '${server.request}'.")
                .handleErrorWith: t =>
                  server
                    .cleanup(false)
                    .flatMap: _ =>
                      F.raiseError(t)
            .handleLeft: err =>
              unauthorized(err)
    case req @ GET -> Root / "login" =>
      ok(loginPage(None, req))
    case req @ POST -> Root / "authenticate" =>
      val remoteAddress = Proxies2.realAddress(req)
      req
        .attemptAs[CloudCreds]
        .foldF(
          err =>
            log.warn(s"Authentication failed from '$remoteAddress'.")
            badRequestEntity(loginPage(Option(err.message), req))
          ,
          ok =>
            val creds = CloudCredentials(ok.cloudID, ok.username, ok.pass, req)
            auth
              .authWebClient(creds)
              .flatMap: e =>
                e.fold(
                  _ =>
                    log.warn(s"Authentication failed from '$remoteAddress'.")
                    badRequestEntity(loginPage(Option("Invalid credentials."), req))
                  ,
                  ok =>
                    val server = creds.cloudID
                    val user = creds.username
                    val who = s"$user@$server"
                    log.info(s"Authentication succeeded to '$who' from '$remoteAddress'.")
                    val redirUri = cookies.intendedUri(req).getOrElse(reverse.root)
                    seeOther(redirUri).map: res =>
                      cookies.withUser[UserPayload](
                        UserPayload(Username.unsafe(server.id)),
                        Proxies2.isSecure(req),
                        res
                      )
                )
        )
    case req @ GET -> Root / "oauth" =>
      google.startHinted(Google, google.google, req)
    case req @ GET -> Root / "oauthcb" =>
      google.googleCallback(req)
    case req @ GET -> Root / "admin" =>
      google.authed(req): _ =>
        ok(html.admin)
    case req @ GET -> Root / "admin" / "logs" =>
      google.authed(req): _ =>
        ok(html.logs)
    case req @ GET -> Root / "admin" / "eject" =>
      google.authed(req): _ =>
        ok(html.eject(req.feedback))
    case req @ GET -> Root / "admin" / "logout" =>
      google.authed(req): _ =>
        seeOther(reverse.admin.eject)
          .withFeedbackMessage("You have now logged out.")
          .map(res => google.logout(cookies.clearSession(res)))
    case req @ POST -> Root / "push" =>
      ok(Json.obj("message" -> "Not supported yet.".asJson))

  import cats.syntax.all.toSemigroupKOps

  def allRoutes(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    routes.combineK(sockets.routes(builder))

  private def withTrack(req: Request[F], id: TrackID)(
    code: (PhoneConnection[F], Track) => F[Response[F]]
  ) =
    proxied(req): phone =>
      phone
        .meta(id)
        .flatMap: outcome =>
          outcome
            .map: track =>
              code(phone, track)
            .handleLeft: err =>
              log.error(s"Found no info about track '$id', failing request.")
              badGatewayDefault

  private def paginated(req: Request[F], cmd: String) = proxied(req): phone =>
    ItemLimits
      .fromReq(req)
      .map: limits =>
        phone
          .jsonRequest(cmd, limits)
          .flatMap: json =>
            ok(json)
      .handleLeft: err =>
        badRequest(err.message.message)

  private def proxiedFolder[T: Encoder](req: Request[F], cmd: String, body: T) =
    proxiedBasic[T](req, cmd, body): json =>
      pimpResult(req)(
        html = json
          .as[Directory]
          .fold(
            err => onGatewayParseErrorResult(err),
            dir => ok(html.index(dir, None))
          ),
        json = ok(json)
      )

  private def proxiedBody(req: Request[F], cmd: String) =
    req.json.flatMap: body =>
      proxiedJson[Json, Json](req, cmd, body)(res => ok(res))

  private def proxiedCommand(req: Request[F], cmd: String) =
    proxiedBasicJson[Json](req, cmd)(json => ok(json))

  private def proxiedBasicJson[U: Decoder](req: Request[F], cmd: String)(
    code: U => F[Response[F]]
  ) = proxiedJson[Json, U](req, cmd, Json.obj())(code)

  private def proxiedJson[T: Encoder, U: Decoder](req: Request[F], cmd: String, body: T)(
    code: U => F[Response[F]]
  ) =
    proxiedBasic[T](req, cmd, body): json =>
      json.as[U].fold(err => onGatewayParseErrorResult(err), u => code(u))

  private def proxiedBasic[T: Encoder](req: Request[F], cmd: String, body: T)(
    code: Json => F[Response[F]]
  ) =
    proxied(req): phone =>
      phone.jsonRequest(cmd, body).flatMap(code)

  private def proxied(req: Request[F])(code: PhoneConnection[F] => F[Response[F]]) =
    auth.phone
      .authenticate(req)
      .flatMap: outcome =>
        outcome.fold(
          fail =>
            pimpResult(req)(
              html = seeOther(reverse.login).map(res => cookies.withIntendedUri(req.uri, res)),
              json = accessDenied
            ),
          phone => code(phone).handleErrorWith(t => badGatewayDefault)
        )

  private def loginPage(formError: Option[String], req: Request[F]) =
    html.login(formError, req.userFeedback(AccountKeys.feedback).map(_.message), None)

  def onGatewayParseErrorResult(err: DecodingFailure): F[Response[F]] =
    log.error(s"Parse error. $err")
    badGateway("A dependent server returned unexpected data.")

  private def unauthorized(failure: Http4sAuthFailure) =
    val req = failure.req
    val ip = Proxies2.realAddress(req)
    val resource = req.uri
    log.warn(s"Unauthorized request to '$resource' from '$ip'.")
    unauthorizedNoCacheWithErrors(Errors("Unauthorized."))
