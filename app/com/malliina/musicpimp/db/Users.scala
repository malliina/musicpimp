package com.malliina.musicpimp.db

import com.malliina.musicpimp.auth.DataUser
import com.malliina.musicpimp.models.User
import slick.driver.H2Driver.api._

class Users(tag: Tag) extends Table[DataUser](tag, "USERS") {
  def user = column[User]("USER", O.PrimaryKey)

  def passHash = column[String]("PASS_HASH")

  def * = (user, passHash) <>((DataUser.apply _).tupled, DataUser.unapply)
}
