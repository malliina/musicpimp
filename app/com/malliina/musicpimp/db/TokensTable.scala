package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.Mappings.username
import com.malliina.play.auth.Token
import com.malliina.play.models.Username
import slick.driver.H2Driver.api._

class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
  def user = column[Username]("USER")

  def series = column[Long]("SERIES")

  def token = column[Long]("TOKEN")

  def * = (user, series, token) <> ((TokensTable.fromRaw _).tupled, TokensTable.write)
}

object TokensTable {
  def fromRaw(user: Username, series: Long, token: Long): Token =
    Token(user, series, token)

  def write(t: Token): Option[(Username, Long, Long)] =
    Option((t.user, t.series, t.token))
}
