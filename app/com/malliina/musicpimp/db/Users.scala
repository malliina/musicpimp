package com.malliina.musicpimp.db

import com.malliina.musicpimp.auth.DataUser
import com.malliina.musicpimp.models.User

import scala.slick.driver.H2Driver.simple._

class Users(tag: Tag) extends Table[DataUser](tag, "USERS") {
  def user = column[User]("USER", O.PrimaryKey, O.NotNull)

  def passHash = column[String]("PASS_HASH", O.NotNull)

  def * = (user, passHash) <>((DataUser.apply _).tupled, DataUser.unapply)
}
