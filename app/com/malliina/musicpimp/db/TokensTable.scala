package com.malliina.musicpimp.db

import com.malliina.play.auth.Token
import slick.driver.H2Driver.api._

class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
  def user = column[String]("USER")

  def series = column[Long]("SERIES")

  def token = column[Long]("TOKEN")

  def * = (user, series, token) <>((Token.apply _).tupled, Token.unapply)
}
