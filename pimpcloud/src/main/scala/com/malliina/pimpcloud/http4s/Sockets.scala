package com.malliina.pimpcloud.http4s

import cats.effect.{Async, Ref}
import cats.implicits.{catsSyntaxApplicativeError, toFlatMapOps, toFunctorOps}
import com.malliina.http.Errors
import com.malliina.http4s.SocketBuilder
import com.malliina.logback.fs2.DefaultFS2IOAppender
import com.malliina.musicpimp.audio.WelcomeMessage
import com.malliina.musicpimp.auth.{Auth2, Http4sAuthFailure, InvalidCredentials, MissingCredentials, Proxies2}
import com.malliina.musicpimp.cloud.{Constants, PimpServerSocket}
import com.malliina.musicpimp.json.{CommonMessages, Target}
import com.malliina.musicpimp.models.{CloudID, JVMLogEntry, SimpleCommand}
import com.malliina.pimpcloud.auth.CloudAuthentication
import com.malliina.pimpcloud.http4s.Sockets.log
import com.malliina.pimpcloud.json.JsonStrings
import com.malliina.pimpcloud.json.JsonStrings.{Body, Cmd, Id, UsernameKey}
import com.malliina.pimpcloud.ws.ServerActor
import com.malliina.pimpcloud.ws.ServerMediator.ServerEvent
import com.malliina.pimpcloud.{PimpList, PimpPhone, PimpPhones, PimpServer, PimpServers, PimpStreams}
import com.malliina.util.AppLogger
import com.malliina.web.Utils
import fs2.concurrent.Topic
import fs2.{Pipe, Stream}
import io.circe.syntax.EncoderOps
import io.circe.{Json, parser}
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import org.http4s.{HttpRoutes, Request}

import java.util.UUID
import scala.concurrent.duration.DurationInt

case class PhoneEndpoint2[F[_]](id: String, server: CloudID, remoteAddress: String)

case class PhoneListener[F[_]](req: Request[?], out: Target[F])

object Sockets:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](
    auth: CloudAuthentication[F],
    servers: Servers[F],
    google: GoogleAuth[F],
    appender: DefaultFS2IOAppender[F]
  ): F[Sockets[F]] =
    for
      phones <- Ref.of[F, Set[PimpPhone]](Set.empty)
      listeners <- Ref.of[F, Set[PhoneListener[F]]](Set.empty)
      serverEvents <- Topic[F, ServerEvent]
      updates <- Topic[F, PimpList]
    yield Sockets(
      auth,
      phones,
      listeners,
      serverEvents,
      servers,
      updates,
      google,
      appender
    )

class Sockets[F[_]: Async](
  auth: CloudAuthentication[F],
  phones: Ref[F, Set[PimpPhone]],
  listeners: Ref[F, Set[PhoneListener[F]]],
  serverEvents: Topic[F, ServerEvent],
  servers: Servers[F],
  updates: Topic[F, PimpList],
  google: GoogleAuth[F],
  appender: DefaultFS2IOAppender[F]
) extends SocketBuilder[F]
  with CloudImplicits[F]:
  val F = Async[F]
  private val events = serverEvents.subscribe(100)

  def connectedServers: F[Set[PimpServerSocket[F]]] = servers.connectedServers

  lazy val jsonEvents = appender.logEvents
    .map: e =>
      JVMLogEntry(
        e.level.levelStr,
        e.message,
        e.loggerName,
        e.threadName,
        e.timeFormatted,
        e.stackTrace
      )
    .groupWithin(5, 100.millis)
    .filter(_.nonEmpty)

  private def welcome = Stream
    .emit[F, Json](WelcomeMessage.asJson)
    .delayBy(100.millis)

  def routes(builder: WebSocketBuilder2[F]): HttpRoutes[F] =
    HttpRoutes.of[F]:
      case req @ GET -> (Root / "mobile" / "ws" | Root / "mobile" / "ws2" |
          Root / "ws" / " playback" | Root / "ws" / "playback2") =>
        auth
          .authPhone(req)
          .flatMap: outcome =>
            outcome
              .map: conn =>
                val id = Utils.randomString()
                phones
                  .updateAndGet(set =>
                    set ++ Set(PimpPhone(id, conn.server.id, Proxies2.realAddress(req)))
                  )
                  .flatMap(set => updates.publish1(PimpPhones(set.toList.sortBy(_.address))).void)
                  .flatMap: _ =>
                    Topic[F, Json].flatMap: target =>
                      val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
                        case Text(message, _) =>
                          parser
                            .parse(message)
                            .map: json =>
                              val isStatus =
                                parser.decode[SimpleCommand](message).contains(SimpleCommand.status)
                              if isStatus then
                                conn
                                  .status()
                                  .flatMap(target.publish1)
                                  .void
                                  .handleErrorWith(t =>
                                    F.delay(log.warn("Status request failed.", t))
                                  )
                              else
                                val payload = Json.obj(
                                  Cmd -> JsonStrings.Player.asJson,
                                  Body -> json,
                                  UsernameKey -> conn.user.asJson
                                )
                                conn.server.send(payload).void
                            .handleLeft: err =>
                              F.delay(log.info(s"Not JSON: '$message'."))
                        case f => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
                      val toClient: Stream[F, WebSocketFrame] =
                        events
                          .filter(_.from == conn.server.id)
                          .map(e => e.message)
                          .mergeHaltBoth(healthChecks)
                          .mergeHaltBoth(target.subscribe(100))
                          .mergeHaltL(welcome)
                          .map(json => Text(json.noSpaces))
                      builder
                        .withOnClose(
                          phones
                            .updateAndGet(set => set.filterNot(_.id == id))
                            .flatMap(set =>
                              updates.publish1(PimpPhones(set.toList.sortBy(_.address))).void
                            )
                        )
                        .build(toClient, fromClient)
              .handleLeft: err =>
                unauthorizedNoCacheWithErrors(Errors.single("Unauthorized."))
      case req @ GET -> (Root / "servers" / "ws" | Root / "servers" / "ws2") =>
        authServer(req).flatMap: outcome =>
          outcome
            .map: cloudId =>
              Topic[F, Json].flatMap: target =>
                val server = PimpServerSocket[F](
                  Target(json => target.publish1(json).void),
                  cloudId,
                  req,
                  servers.connectedServers.flatMap(ss => updates.publish1(snapshots(ss)).void)
                )
                servers
                  .connected(server)
                  .flatMap(set =>
                    updates
                      .publish1(
                        PimpServers(
                          set.toList
                            .map(s => PimpServer(s.id, Proxies2.realAddress(req)))
                            .sortBy(_.address)
                        )
                      )
                      .void
                  )
                  .flatMap: _ =>
                    val registered = Stream
                      .emit[F, Json](
                        Json.obj(
                          Cmd -> ServerActor.RegisteredKey.asJson,
                          Body -> Json.obj(Id -> cloudId.asJson)
                        )
                      )
                    val toClient: Stream[F, WebSocketFrame] =
                      registered.map(json => Text(json.noSpaces))
                    val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
                      case Text(message, _) =>
                        parser
                          .parse(message)
                          .map: json =>
                            if json == CommonMessages.ping then
                              target.publish1(CommonMessages.pong).void
                            else
                              server
                                .complete(json)
                                .flatMap: completed =>
                                  if !completed then
                                    serverEvents.publish1(ServerEvent(json, cloudId)).void
                                  else F.unit
                          .handleLeft: err =>
                            F.delay(log.warn(s"Not JSON: '$message'. $err"))
                      case f => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
                    builder
                      .withOnClose(
                        servers
                          .disconnected(cloudId)
                          .flatMap(set =>
                            updates
                              .publish1(
                                PimpServers(
                                  set
                                    .map(s => PimpServer(s.id, s.address))
                                    .toList
                                    .sortBy(_.address)
                                )
                              )
                              .void
                          )
                      )
                      .build(toClient, fromClient)
            .handleLeft: err =>
              unauthorizedNoCacheWithErrors(Errors.single("Missing credentials."))
      case req @ GET -> Root / "admin" / "ws" =>
        google.authed(req): _ =>
          val toClient = jsonEvents.map(chunk => Text(chunk.toList.asJson.noSpaces))
          val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
            case Text(message, _) => F.delay(log.debug(s"Client says '$message'."))
            case f                => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
          builder.build(toClient, fromClient)
      case req @ GET -> Root / "admin" / "usage" =>
        google.authed(req): _ =>
          val toClient = updates
            .subscribe(100)
            .map(ss => ss.asJson)
            .mergeHaltBoth(healthChecks)
            .mergeHaltL(currentState.map(_.asJson))
            .map(json => Text(json.noSpaces))
          val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap:
            case Text(message, _) =>
              F.delay(log.info(s"Client '${Proxies2.realAddress(req)}' says '$message'."))
            case f => F.delay(log.warn(s"Unknown WebSocket frame: $f"))
          builder.build(toClient, fromClient)

  private def currentState: Stream[F, PimpList] =
    val state = for
      ps <- phones.get.map(ps => PimpPhones(ps.toList.sortBy(_.address)))
      ss <- servers.connectedServers
    yield
      val pss = PimpServers(ss.map(s => PimpServer(s.id, s.address)).toList.sortBy(_.address))
      List(ps, pss, snapshots(ss))
    Stream.evals(state)

  private def snapshots(ss: Set[PimpServerSocket[F]]): PimpStreams =
    val snaps = ss.flatMap(_.fileTransfers.snapshot)
    PimpStreams(snaps.toList.sortBy(snap => (snap.serverID.id, snap.request.id)))

  private def authServer(req: Request[?]): F[Either[Http4sAuthFailure, CloudID]] =
    Auth2
      .basicCredentials(req.headers)
      .map: creds =>
        if creds.password == Constants.pass then
          val user = creds.username
          val cloudId = if user.name.trim.nonEmpty then CloudID(user.name) else newID()
          connectedServers.map: existing =>
            val idInUse = existing.exists(_.id == cloudId)
            if idInUse then
              log.warn(
                s"Unable to register client: '$cloudId'. Another client with that ID is already connected."
              )
              Left(InvalidCredentials(req))
            else Right(cloudId)
        else F.pure(Left(InvalidCredentials(req)))
      .getOrElse:
        log.warn(s"No credentials for request from '${Proxies2.realAddress(req)}'.")
        F.pure(Left(MissingCredentials(req)))

  private def newID(): CloudID = CloudID(UUID.randomUUID().toString take 5)
