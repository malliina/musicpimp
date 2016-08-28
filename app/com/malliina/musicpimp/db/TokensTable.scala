package com.malliina.musicpimp.db

import com.malliina.play.auth.Token
import com.malliina.play.models.Username
import slick.driver.H2Driver.api._

class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
  def user = column[String]("USER")

  def series = column[Long]("SERIES")

  def token = column[Long]("TOKEN")

  def * = (user, series, token) <> ((TokensTable.fromRaw _).tupled, TokensTable.write)
}

object TokensTable {
  def fromRaw(user: String, series: Long, token: Long): Token =
    Token(Username(user), series, token)

  def write(t: Token): Option[(String, Long, Long)] = Option((t.user.name, t.series, t.token))
}
