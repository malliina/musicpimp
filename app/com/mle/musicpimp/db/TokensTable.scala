package com.mle.musicpimp.db

import com.mle.play.auth.Token

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class TokensTable(tag: Tag) extends Table[Token](tag, "TOKENS") {
  def user = column[String]("USER")

  def series = column[Long]("SERIES")

  def token = column[Long]("TOKEN")

  def * = (user, series, token) <>((Token.apply _).tupled, Token.unapply)
}
