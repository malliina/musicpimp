package com.malliina.musicpimp.cloud

import play.api.libs.json.Json

object PimpMessages {

  trait PimpMessage

  case object Ping extends PimpMessage

  case object PingAuth extends PimpMessage

  case object RootFolder extends PimpMessage

  case class Folder(id: String) extends PimpMessage

  case class Track(id: String) extends PimpMessage

  case class Search(term: String, limit: Int) extends PimpMessage

  case object Alarms extends PimpMessage

  case class Version(version: String) extends PimpMessage

  case class Failure(reason: String) extends PimpMessage

  implicit val searchFormat = Json.format[Search]
  implicit val folderFormat = Json.format[Folder]
  implicit val trackFormat = Json.format[Track]
  implicit val versionFormat = Json.format[Version]
}
