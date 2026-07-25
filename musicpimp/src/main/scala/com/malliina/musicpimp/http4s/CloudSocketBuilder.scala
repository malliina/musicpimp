package com.malliina.musicpimp.http4s

import cats.effect.Sync
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.auth.AuthedRequest
import com.malliina.musicpimp.cloud.Clouds
import com.malliina.musicpimp.http4s.CloudSocketBuilder.log
import com.malliina.musicpimp.models.{CloudCommand, Connect, Disconnect, Noop}
import com.malliina.util.AppLogger
import io.circe.parser.parse
import io.circe.syntax.EncoderOps
import org.http4s.Response
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

object CloudSocketBuilder:
  private val log = AppLogger(getClass)

class CloudSocketBuilder[F[_]: Sync](clouds: Clouds[F]):
  val F = Sync[F]

  def flow(req: AuthedRequest[F], builder: WebSocketBuilder2[F]): F[Response[F]] =
    val toClient = clouds.connection
      .map: json =>
        Text(json.asJson.noSpaces)
    val fromClient: fs2.Pipe[F, WebSocketFrame, Unit] = _.evalMap:
      case Text(message, _) =>
        parse(message).fold(
          fail => F.delay(log.error(s"Failed to parse as JSON: '$message")),
          ok =>
            ok.as[CloudCommand]
              .fold(
                err => F.delay(log.error(s"Invalid JSON '$message'. $err")),
                cmd => handleCommand(cmd)
              )
        )
      case f => F.delay(log.debug(s"Unknown WebSocket frame: $f"))
    builder
      .build(toClient, fromClient)
      .flatMap: res =>
        clouds.emitLatest().map(_ => res)

  private def handleCommand(cmd: CloudCommand): F[Unit] =
    cmd match
      case Connect(id) => clouds.connect(Option(id).filter(_.id.nonEmpty)).void
      case Disconnect  => clouds.disconnectAndForgetAsync().void
      case Noop        => F.unit
