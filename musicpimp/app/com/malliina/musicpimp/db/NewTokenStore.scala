package com.malliina.musicpimp.db

import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.values.Username

import scala.concurrent.Future

class NewTokenStore(db: PimpMySQL) extends TokenStore {
  override implicit val ec = db.ec
  import db._

  override def persist(token: Token): Future[Unit] = removal {
    runIO(
      tokensTable.insert(
        _.user -> lift(token.user),
        _.series -> lift(token.series),
        _.token -> lift(token.token)
      )
    )
  }

  override def removeAll(user: Username): Future[Unit] = removal {
    runIO(tokensTable.filter(_.user == lift(user)).delete)
  }

  override def remove(token: Token): Future[Unit] = removal {
    runIO(
      tokensTable.filter { t =>
        t.user == lift(token.user) && t.series == lift(token.series) && t.token == lift(
          token.token
        )
      }.delete
    )
  }
  override def remove(user: Username, series: Long): Future[Unit] = removal {
    runIO(
      tokensTable
        .filter(t => t.user == lift(user) && t.series == lift(series))
        .delete
    )
  }

  override def findToken(user: Username, series: Long): Future[Option[Token]] =
    performAsync("Find token") {
      runIO(tokensTable.filter(t => t.user == lift(user) && t.series == lift(series)))
        .map(_.headOption)
    }

  private def removal[T](io: IO[T, Effect.Write]): Future[Unit] =
    performAsync[Unit]("Token removal") { io.map(_ => ()) }
}
