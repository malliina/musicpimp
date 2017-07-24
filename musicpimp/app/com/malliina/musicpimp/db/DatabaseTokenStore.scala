package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.Mappings.username
import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.play.models.Username
import slick.jdbc.H2Profile.api._

import scala.concurrent.{ExecutionContext, Future}

class DatabaseTokenStore(db: PimpDb, val ec: ExecutionContext)
  extends Sessionizer(db)
    with TokenStore {

  val tokens = PimpSchema.tokens

  override def persist(token: Token): Future[Unit] =
    run(tokens += token).map(_ => ())(ec)

  override def removeAll(user: Username): Future[Unit] =
    removeWhere(_.user === user)

  override def remove(token: Token): Future[Unit] =
    removeWhere(t => t.user === token.user && t.series === token.series && t.token === token.token)

  override def remove(user: Username, series: Long): Future[Unit] =
    removeWhere(t => t.user === user && t.series === series)

  override def findToken(user: Username, series: Long): Future[Option[Token]] =
    run(tokens.filter(t => t.user === user && t.series === series).result.headOption)

  private def removeWhere(p: TokensTable => Rep[Boolean]): Future[Unit] =
    run(tokens.filter(p).delete).map(_ => ())(ec)
}
