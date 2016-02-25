package com.malliina.musicpimp.models

import scala.slick.driver.H2Driver.simple.{MappedColumnType, stringColumnType}

case class User(name: String) {
  override def toString = name
}

object User extends SimpleCompanion[String, User] {
  implicit val db = MappedColumnType.base[User, String](raw, apply)

  override def raw(t: User): String = t.name
}
