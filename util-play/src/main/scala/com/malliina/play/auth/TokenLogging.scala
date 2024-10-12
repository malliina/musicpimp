package com.malliina.play.auth

import com.malliina.play.auth.TokenLogging.log
import play.api.Logger

import scala.concurrent.Future

trait TokenLogging extends TokenStore {
  abstract override def persist(token: Token): Future[Unit] =
    super.persist(token) map { _ => log.debug(s"Persisted token: $token") }

  abstract override def remove(token: Token): Future[Unit] =
    super.remove(token) map { _ => log.debug(s"Removed token: $token") }
}

object TokenLogging {
  val log = Logger(getClass)
}
