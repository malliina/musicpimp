package com.malliina.play.auth

import cats.effect.IO
import com.malliina.play.auth.TokenLogging.log
import play.api.Logger

trait TokenLogging extends TokenStore[IO]:
  abstract override def persist(token: Token): IO[Unit] =
    super
      .persist(token)
      .map: _ =>
        log.debug(s"Persisted token: $token")

  abstract override def remove(token: Token): IO[Unit] =
    super
      .remove(token)
      .map: _ =>
        log.debug(s"Removed token: $token")

object TokenLogging:
  val log = Logger(getClass)
