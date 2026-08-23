package com.malliina.beam.http4s

import cats.effect.Async
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.malliina.auth.BasicCredentials
import com.malliina.beam.BeamStrings.{BEAM_HOST, PORT, SSL_PORT, SUPPORTS_PLAINTEXT, SUPPORTS_TLS, USER}
import com.malliina.beam.http4s.Service.log
import com.malliina.beam.{BeamConf, BeamMessage, BeamMessages, BeamState, BeamTags, BuildMeta, DiscoGs, PhoneClient, PlayerClient}
import com.malliina.http.{Errors, HttpHeaders}
import com.malliina.http4s.PimpExt.handleLeft
import com.malliina.http4s.{AppImplicits, SocketBuilder}
import com.malliina.musicpimp.auth.{Auth2, Http4sAuth, Proxies2, UserPayload}
import com.malliina.musicpimp.json.{SharedJsonMessages, Target}
import com.malliina.storage.{StorageInt, StorageLong, StorageSize}
import com.malliina.util.AppLogger
import com.malliina.values.{Password, Username}
import fs2.concurrent.Topic
import fs2.io.file.Files
import fs2.{Pipe, Stream}
import io.circe.Json
import io.circe.syntax.EncoderOps
import net.glxn.qrgen.QRCode
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.uri
import org.http4s.multipart.Part
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{EntityDecoder, Header, HttpRoutes, MediaType, Request, Response, Status}
import org.typelevel.ci.CIStringSyntax

import java.util.UUID
import scala.concurrent.duration.DurationInt

object Service:
  private val log = AppLogger(getClass)

class Service[F[_]: { Async, Files }](
  state: BeamState[F],
  disco: DiscoGs[F],
  cookies: Http4sAuth[F],
  conf: BeamConf
) extends SocketBuilder[F]
  with AppImplicits[F]:
  val F = Async[F]
  log.info(
    s"Started MusicBeamer endpoint. Advertising address: ${conf.host}:${conf.port}/${conf.sslPort}."
  )

  private val routes: HttpRoutes[F] = HttpRoutes.of[F]:
    case req @ GET -> Root =>
      /** The landing page of the browser, after which image is called, after which stream is called
        * by the browser if a mobile device has happened to connect and started streaming something.
        */
      val user = UUID.randomUUID().toString
      val remoteIP = Proxies2.realAddress(req)
      log.info(s"Created user '$user' from '$remoteIP'.")
      ok(BeamTags.index).map: res =>
        cookies.withUser(UserPayload(Username.unsafe(user)), Proxies2.isSecure(req), res)
    case req @ GET -> Root / "health" =>
      ok(BuildMeta.default)
    case req @ GET -> Root / "ping" =>
      ok(BuildMeta.default)
    case req @ GET -> Root / "image" =>
      /** Not sure if the websocket connection has been opened by the time the call to this resource
        * is made, hence the "Limited" security check, but it matters little.
        */
      authUser(req): user =>
        val qrText = coordinate(user).asJson.noSpaces
        log.info(s"Generating image with QR code: $qrText")
        val qrFile = QRCode.from(qrText).withSize(768, 768).file()
        ok(qrFile.toPath)
    case req @ GET -> Root / "covers" / artist / album =>
      /** Loads an album cover from DiscoGs into memory, then sends it to the client.
        *
        * Set the route that calls this action as the `src` attribute of an `img` element to display
        * the cover.
        *
        * @return
        *   an action that returns the cover of `artist` s `album`
        */
      authUser(req): user =>
        log.info(s"Searching for cover: $artist - $album")
        disco
          .request(artist, album)
          .map: cover =>
            Response(Status.Ok, body = fs2.Stream.emits(cover.body)).putHeaders(
              Header.Raw(
                ci"Content-Type",
                cover.headers
                  .get(HttpHeaders.`Content-Type`)
                  .flatMap(_.headOption)
                  .getOrElse("application/octet-stream")
              )
            )
          .handleErrorWith: err =>
            seeOther(uri"/assets/img/guitar.png")
    case req @ GET -> Root / "streamable" =>
      authPhone(req): user =>
        state
          .find(user)
          .flatMap: maybePlayer =>
            val json = BeamMessages.playerExists(user, maybePlayer.isDefined, true)
            ok(json)
    case req @ GET -> Root / "stream" =>
      authPlayer(req): player =>
        log.info(s"Sending stream to player '${player.user}'...")
        ok(player.stream).map: res =>
          res.withContentType(`Content-Type`(MediaType.audio.mpeg))
    case req @ POST -> Root / "stream" =>
      /** Replaces the playlist with the uploaded file.
        */
      authPlayer(req): player =>
        player.resetStream.flatMap: _ =>
          tryPushFile(player, req)
    case req @ POST -> Root / "stream" / "tail" =>
      /** Adds the uploaded file to the playlist.
        */
      authPlayer(req): player =>
        tryPushFile(player, req)

  private def welcome = Stream
    .emit[F, Json](SharedJsonMessages.welcome)
    .delayBy(100.millis)

  private def socketRoutes(builder: WebSocketBuilder2[F]) = HttpRoutes.of[F]:
    case req @ GET -> Root / "ws" / "control" => // isPhone
      authPhone(req): user =>
        Topic[F, Json].flatMap: target =>
          val phone = PhoneClient(user, Target(json => target.publish1(json).void))
          state
            .phoneConnected(phone)
            .flatMap: _ =>
              val toClient = healthChecks
                .mergeHaltBoth(target.subscribe(100))
                .mergeHaltBoth(
                  state.messages.filter(msg => msg.user == user && msg.toPhone).map(_.message)
                )
                .mergeHaltL(welcome)
                .map(json => Text(json.noSpaces))
              val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
                case Text(message, _) =>
                  io.circe.parser
                    .parse(message)
                    .map: json =>
                      state.send(BeamMessage(json, user, true)).void
                    .handleLeft: err =>
                      F.delay(log.info(s"Not JSON: '$message'. $err"))
                case f => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
              builder.withOnClose(state.phoneDisconnected(phone).void).build(toClient, fromClient)
    case req @ GET -> Root / "ws" / "player" => // isPlayer
      authUser(req): user =>
        Topic[F, Json].flatMap: target =>
          PlayerClient
            .default(user, Target(json => target.publish1(json).void))
            .flatMap: player =>
              state
                .connected(player)
                .flatMap: _ =>
                  val toClient = healthChecks
                    .mergeHaltBoth(target.subscribe(100))
                    .mergeHaltBoth(
                      state.messages.filter(msg => msg.user == user && msg.toPlayer).map(_.message)
                    )
                    .mergeHaltL(welcome)
                    .map(json => Text(json.noSpaces))
                  val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
                    case Text(message, _) =>
                      io.circe.parser
                        .parse(message)
                        .map: json =>
                          state.send(BeamMessage(json, user, false)).void
                        .handleLeft: err =>
                          F.delay(log.info(s"Not JSON: '$message'. $err"))
                    case f => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
                  builder.withOnClose(state.disconnected(player).void).build(toClient, fromClient)

  import cats.syntax.all.toSemigroupKOps

  def allRoutes(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    routes.combineK(socketRoutes(builder))

  private val maxSize: StorageSize = 1024.megs

  private def tryPushFile(player: PlayerClient[F], req: Request[F]) =
    log.info(s"Serving file to user '${player.user}'...")
    val remoteIP = Proxies2.realAddress(req)
    EntityDecoder
      .mixedMultipartResource[F]()
      .use: decoder =>
        log.info(s"Streaming at most $maxSize from $remoteIP to '${player.user}'...")
        req.decodeWith(decoder, true): parts =>
          val consume = fs2.Stream
            .emits[F, Part[F]](parts.parts.filter(_.filename.isDefined))
            .mapAsync(1): filePart =>
              filePart.body
                .chunkN(4096, allowFewer = true)
                .evalMapAccumulate(0.bytes): (acc, chunk) =>
                  player
                    .send(chunk.toArray)
                    .map: b =>
                      ((acc.bytes + chunk.size).bytes, b)
                .compile
                .last
                .map(_.getOrElse((0.bytes, false)))
          consume.compile.toList.flatMap: results =>
            for
              _ <- player.close
              result <-
                val streamedSize =
                  results.foldLeft(0L)((acc, part) => acc + part._1.bytes).bytes
                val fileCount = results.size
                val fileDesc = if fileCount > 1 then "files" else "file"
                val remoteIP = Proxies2.realAddress(req)
                log.info(
                  s"Streamed $streamedSize in $fileCount $fileDesc from $remoteIP to '${player.user}'."
                )
                ok(Json.obj("message" -> "ok".asJson))
            yield result

  private def coordinate(user: Username): Json = Json.obj(
    BEAM_HOST -> conf.host.asJson,
    PORT -> conf.port.asJson,
    SSL_PORT -> conf.sslPort.asJson,
    USER -> user.asJson,
    SUPPORTS_PLAINTEXT -> false.asJson,
    SUPPORTS_TLS -> true.asJson
  )

  private def authPlayer(req: Request[F])(code: PlayerClient[F] => F[Response[F]]) =
    authUser(req): user =>
      state
        .find(user)
        .flatMap: playerOpt =>
          playerOpt.map(code).getOrElse(notFound("Player not found."))

  private def authUser(req: Request[F])(code: Username => F[Response[F]]) =
    cookies
      .auth(req)
      .map: user =>
        code(user.username)
      .handleLeft: err =>
        unauthorizedNoCacheWithErrors(Errors.single("Unauthorized."))

  private def authPhone(req: Request[F])(code: Username => F[Response[F]]): F[Response[F]] =
    Auth2
      .basicCredentials(req.headers)
      .map: creds =>
        if validateCredentials(creds) then code(creds.username)
        else unauthorizedNoCacheWithErrors(Errors.single("Unauthorized."))
      .getOrElse:
        unauthorizedNoCacheWithErrors(Errors.single("Credentials missing."))

  def validateCredentials(creds: BasicCredentials): Boolean =
    val user = creds.username
    // the password is not really a secret
    val credsOk = user.name.nonEmpty && Password.build("beam").exists(_ == creds.password)
    if !credsOk then log.warn(s"Invalid credentials provided as user '$user'.")
    credsOk
