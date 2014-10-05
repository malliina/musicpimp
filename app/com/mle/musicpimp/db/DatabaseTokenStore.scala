package com.mle.musicpimp.db

import com.mle.play.auth.{Token, TokenStore}

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class DatabaseTokenStore(db: PimpDatabase) extends TokenStore {
  val tokens = db.tokens

  override def persist(token: Token): Unit = withSession(implicit s => tokens += token)

  override def removeAll(user: String): Unit = removeWhere(_.user === user)

  override def remove(token: Token): Unit =
    removeWhere(t => t.user === token.user && t.series === token.series && t.token === token.token)

  override def remove(user: String, series: Long): Unit = removeWhere(t => t.user === user && t.series === series)

  override def findToken(user: String, series: Long): Option[Token] =
    withSession(tokens.filter(t => t.user === user && t.series === series).firstOption(_))

  private def removeWhere(p: TokensTable => Column[Boolean]) = withSession(tokens.filter(p).delete(_))

  private def withSession[T](body: Session => T) = db.withSession(s => body(s))
}

object DatabaseTokenStore extends DatabaseTokenStore(PimpDb)