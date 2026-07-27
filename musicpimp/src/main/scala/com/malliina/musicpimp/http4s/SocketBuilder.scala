package com.malliina.musicpimp.http4s

import cats.effect.Temporal
import com.malliina.play.json.JsonMessages
import fs2.Stream

import scala.concurrent.duration.DurationInt

class SocketBuilder[F[_]: Temporal]:
  val healthChecks =
    Stream.awakeEvery[F](30.seconds).delayBy(10.seconds).map(_ => JsonMessages.ping)
