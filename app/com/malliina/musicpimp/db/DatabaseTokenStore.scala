package com.malliina.musicpimp.db

import com.malliina.play.auth.{Token, TokenStore}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class DatabaseTokenStore(db: PimpDb) extends Sessionizer(db) with TokenStore {
  val tokens = PimpSchema.tokens

  override def persist(token: Token): Future[Unit] = run(tokens += token).map(_ => ())

  override def removeAll(user: String): Future[Unit] = removeWhere(_.user === user)

  override def remove(token: Token): Future[Unit] =
    removeWhere(t => t.user === token.user && t.series === token.series && t.token === token.token)

  override def remove(user: String, series: Long): Future[Unit] = removeWhere(t => t.user === user && t.series === series)

  override def findToken(user: String, series: Long): Future[Option[Token]] =
    run(tokens.filter(t => t.user === user && t.series === series).result.headOption)

  private def removeWhere(p: TokensTable => Rep[Boolean]): Future[Unit] =
    run(tokens.filter(p).delete).map(_ => ())
}
