package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.{FolderID, TrackID}
import play.api.libs.json.Json

trait PimpMessage

trait BaseMessages {

  case object PingMessage extends PimpMessage

  case object PingAuth extends PimpMessage

  case object RootFolder extends PimpMessage

  case class GetFolder(id: FolderID) extends PimpMessage

  case class Track(id: TrackID) extends PimpMessage

  case class Search(term: String, limit: Int) extends PimpMessage

  case object GetAlarms extends PimpMessage

  implicit val searchFormat = Json.format[Search]
  implicit val folderFormat = Json.format[GetFolder]
  implicit val trackFormat = Json.format[Track]
}
