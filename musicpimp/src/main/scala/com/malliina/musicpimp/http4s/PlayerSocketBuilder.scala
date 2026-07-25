package com.malliina.musicpimp.http4s

import cats.effect.Async
import com.malliina.musicpimp.audio.{JsonHandlerBase, PingEvent, ServerMessage, ServerPlayer, TimeUpdatedMessage, TrackJson, TrackMeta, WelcomeMessage}
import com.malliina.musicpimp.auth.AuthedRequest
import com.malliina.util.AppLogger
import fs2.Stream
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text
import PlayerSocketBuilder.log
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.FullUrls2
import fs2.concurrent.Topic
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps
import io.circe.parser.parse
import org.http4s.Response

import scala.concurrent.duration.DurationInt

object PlayerSocketBuilder:
  private val log = AppLogger(getClass)

class PlayerSocketBuilder[F[_]: Async](player: ServerPlayer[F], messageHandler: JsonHandlerBase[F])
  extends SocketBuilder[F]:
  val F = Async[F]

  private val pings = Stream.awakeEvery[F](5.seconds).delayBy(1.second).map(_ => PingEvent.asJson)
//  private val ticks = Stream.awakeEvery(900.millis).delayBy(200.millis)

  def playback(req: AuthedRequest[F], builder: WebSocketBuilder2[F]): F[Response[F]] =
    val host = FullUrls2.hostOnly2(req.request)
    val messageWriter = ServerMessage.jsonWriter(using TrackJson.format(host))
    given w: Encoder[TrackMeta] = TrackJson.writer(host)
    var previousPos = -1L
    Topic[F, Json].flatMap: target =>
      def welcome = Stream
        .emit[F, Json](messageWriter(WelcomeMessage))
        .delayBy(100.millis)
      val toClient = pings
        .mergeHaltBoth(healthChecks)
        .mergeHaltBoth(target.subscribe(100))
        .mergeHaltBoth(player.allEvents.map(msg => messageWriter(msg)))
        .mergeHaltL(welcome)
//        .mergeHaltBoth(ticks.evalMapFilter: _ =>
//          val pos = player.position
//          val posSeconds = pos.toSeconds
//          F.delay:
//            if posSeconds != previousPos then
//              previousPos = posSeconds
//              Some(TimeUpdatedMessage(pos).asJson)
//            else None)
        .map: json =>
          Text(json.noSpaces)
      val fromClient: fs2.Pipe[F, WebSocketFrame, Unit] = _.evalMap:
        case Text(message, _) =>
          parse(message).fold(
            fail => F.delay(log.error(s"Failed to parse as JSON: '$message")),
            ok =>
              messageHandler.onJson(
                ok,
                RemoteInfo(
                  req.username,
                  Responses.apiVersion(req.request),
                  FullUrls2.hostOnly2(req.request),
                  Target(json => target.publish1(json).void)
                )
              )
          )
        case f => F.delay(log.debug(s"Unknown WebSocket frame: $f"))
      builder.build(toClient, fromClient)
