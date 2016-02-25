package com.malliina.musicpimp.db

import com.malliina.play.auth.{Token, TokenStore}

import scala.concurrent.Future
import scala.slick.driver.H2Driver.simple._

class DatabaseTokenStore(db: PimpDb) extends Sessionizer(db) with TokenStore {
  val tokens = PimpSchema.tokens

  override def persist(token: Token): Future[Unit] = withSession(implicit s => tokens += token)

  override def removeAll(user: String): Future[Unit] = removeWhere(_.user === user)

  override def remove(token: Token): Future[Unit] =
    removeWhere(t => t.user === token.user && t.series === token.series && t.token === token.token)

  override def remove(user: String, series: Long): Future[Unit] = removeWhere(t => t.user === user && t.series === series)

  override def findToken(user: String, series: Long): Future[Option[Token]] =
    withSession(tokens.filter(t => t.user === user && t.series === series).firstOption(_))

  private def removeWhere(p: TokensTable => Column[Boolean]): Future[Unit] = withSession(tokens.filter(p).delete(_))
}
