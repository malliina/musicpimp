package com.malliina.musicpimp.http4s

import cats.effect.Async
import com.malliina.musicpimp.auth.AuthedRequest
import com.malliina.musicpimp.db.{IndexEvent, Indexer}
import com.malliina.musicpimp.http4s.SearchSocketBuilder.log
import com.malliina.musicpimp.models.{Refresh, SearchMessage, SearchStatus, Subscribe}
import com.malliina.util.AppLogger
import io.circe.parser.decode
import io.circe.syntax.EncoderOps
import org.http4s.Response
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

object SearchSocketBuilder:
  private val log = AppLogger(getClass)

class SearchSocketBuilder[F[_]: Async](indexer: Indexer[F]) extends SocketBuilder[F]:
  val F = Async[F]

  def socket(req: AuthedRequest[F], builder: WebSocketBuilder2[F]): F[Response[F]] =
    val clientMessages = indexer.updates.map:
      case IndexEvent.Started         => "Refresh indexing..."
      case IndexEvent.Finished        => "Indexing complete."
      case IndexEvent.Progress(files) => s"Indexing... $files files indexed so far..."
      case IndexEvent.Errored(t)      => "Indexing errored."
    val toClient = healthChecks
      .mergeHaltBoth(clientMessages.map(msg => SearchStatus(msg).asJson))
      .map: json =>
        Text(json.noSpaces)
    val fromClient: fs2.Pipe[F, WebSocketFrame, Unit] = _.evalMap:
      case Text(message, _) =>
        decode[SearchMessage](message).fold(
          err => F.delay(log.error(s"Failed to decode message: '$message")),
          {
            case Refresh =>
              log.info("Refresh indexing...")
              indexer.submitIndexAndSave()
            case Subscribe => F.unit
          }
        )
      case f => F.delay(log.debug(s"Unknown WebSocket frame: $f"))
    builder.build(toClient, fromClient)
