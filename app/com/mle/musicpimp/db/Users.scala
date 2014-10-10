package com.mle.musicpimp.db

import com.mle.musicpimp.auth.DataUser

import scala.slick.driver.H2Driver.simple._

/**
 * @author Michael
 */
class Users(tag: Tag) extends Table[DataUser](tag, "USERS") {
  def user = column[String]("USER", O.PrimaryKey, O.NotNull)

  def passHash = column[String]("PASS_HASH", O.NotNull)

  def * = (user, passHash) <>((DataUser.apply _).tupled, DataUser.unapply)
}
