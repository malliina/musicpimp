package com.malliina.musicpimp.db

import com.malliina.database.DoobieDatabase
import com.malliina.play.auth.{Token, TokenStore}
import com.malliina.values.Username
import doobie.Fragment
import doobie.implicits.toSqlInterpolator

class DoobieTokenStore[F[_]](val db: DoobieDatabase[F]) extends TokenStore[F] with DoobieMappings:
  def persist(token: Token): F[Unit] = update:
    sql"""insert into TOKENS(user, series, token)
          values (${token.user}, ${token.series}, ${token.token})"""

  def removeAll(user: Username): F[Unit] = update:
    sql"""delete from TOKENS
          where USER = $user"""

  def remove(token: Token): F[Unit] = update:
    sql"""delete from TOKENS
          where USER = ${token.user} and SERIES = ${token.series} and TOKEN = ${token.token}"""

  def remove(user: Username, series: Long): F[Unit] = update:
    sql"""delete from TOKENS
          where USER = $user and SERIES = $series"""

  def findToken(user: Username, series: Long): F[Option[Token]] = db.run:
    sql"""select T.USER, T.SERIES, T.TOKEN
          from TOKENS T
          where T.USER = $user and T.SERIES = $series""".query[Token].option

  private def update(fragment: Fragment): F[Unit] = db.run:
    fragment.update.run.map(_ => ())
