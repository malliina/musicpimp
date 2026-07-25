package com.malliina.musicpimp.http4s

import cats.effect.{Async, Sync}
import com.malliina.logback.fs2.DefaultFS2IOAppender
import com.malliina.musicpimp.auth.AuthedRequest
import fs2.{Pipe, Stream}
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.http4s.Response
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.websocket.WebSocketFrame
import org.http4s.websocket.WebSocketFrame.Text

import scala.concurrent.duration.DurationInt

class LogSocketBuilder[F[_]: Async](appender: DefaultFS2IOAppender[F]):
  private val jsonEvents: Stream[F, Json] =
    appender.logEvents.groupWithin(5, 100.millis).filter(_.nonEmpty).map(e => e.toList.asJson)

  def build(req: AuthedRequest[F], builder: WebSocketBuilder2[F]): F[Response[F]] =
    val toClient = jsonEvents.map: json =>
      Text(json.asJson.noSpaces)
    val fromClient: Pipe[F, WebSocketFrame, Unit] = _.evalMap: msg =>
      Sync[F].unit
    builder.build(toClient, fromClient)
