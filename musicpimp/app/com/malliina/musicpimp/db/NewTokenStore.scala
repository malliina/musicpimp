package com.malliina.musicpimp.db

import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.values.Username
import io.getquill.*
import scala.concurrent.{ExecutionContext, Future}

object NewTokenStore:
  def apply(db: PimpMySQL): NewTokenStore = new NewTokenStore(db)

class NewTokenStore(db: PimpMySQL) extends TokenStore:
  import db.*
  override implicit val ec: ExecutionContext = db.ec

  override def persist(token: Token): Future[Unit] = removal:
    run(
      tokensTable.insert(
        _.user -> lift(token.user),
        _.series -> lift(token.series),
        _.token -> lift(token.token)
      )
    )

  override def removeAll(user: Username): Future[Unit] = removal:
    run(tokensTable.filter(_.user == lift(user)).delete)

  override def remove(token: Token): Future[Unit] = removal:
    run(
      tokensTable
        .filter: t =>
          t.user == lift(token.user) && t.series == lift(token.series) && t.token == lift(
            token.token
          )
        .delete
    )
  override def remove(user: Username, series: Long): Future[Unit] = removal:
    run(
      tokensTable
        .filter(t => t.user == lift(user) && t.series == lift(series))
        .delete
    )

  override def findToken(user: Username, series: Long): Future[Option[Token]] =
    performAsync("Find token"):
      val tokens = run(tokensTable.filter(t => t.user == lift(user) && t.series == lift(series)))
      tokens.headOption

  private def removal[T](code: => Any): Future[Unit] =
    wrapTask[Unit]("Token removal"):
      code
