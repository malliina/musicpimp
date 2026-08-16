package com.malliina.http4s

import cats.effect.Temporal
import com.malliina.play.json.JsonMessages
import fs2.Stream
import io.circe.Json

import scala.concurrent.duration.DurationInt

class SocketBuilder[F[_]: Temporal]:
  val healthChecks: Stream[F, Json] =
    Stream.awakeEvery[F](30.seconds).delayBy(10.seconds).map(_ => JsonMessages.ping)
