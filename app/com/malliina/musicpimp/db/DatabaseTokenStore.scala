package com.malliina.musicpimp.db

import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.play.models.Username
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.H2Driver.api._

import scala.concurrent.Future

class DatabaseTokenStore(db: PimpDb) extends Sessionizer(db) with TokenStore {
  val tokens = PimpSchema.tokens

  override def persist(token: Token): Future[Unit] = run(tokens += token).map(_ => ())

  override def removeAll(user: Username): Future[Unit] = removeWhere(_.user === user.name)

  override def remove(token: Token): Future[Unit] =
    removeWhere(t => t.user === token.user.name && t.series === token.series && t.token === token.token)

  override def remove(user: Username, series: Long): Future[Unit] =
    removeWhere(t => t.user === user.name && t.series === series)

  override def findToken(user: Username, series: Long): Future[Option[Token]] =
    run(tokens.filter(t => t.user === user.name && t.series === series).result.headOption)

  private def removeWhere(p: TokensTable => Rep[Boolean]): Future[Unit] =
    run(tokens.filter(p).delete).map(_ => ())
}
