package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.{FolderID, TrackID}
import play.api.libs.json.Json

trait PimpMessage

case object PingMessage extends PimpMessage

case object PingAuth extends PimpMessage

case object RootFolder extends PimpMessage

case class GetFolder(id: FolderID) extends PimpMessage

object GetFolder {
  implicit val json = Json.format[GetFolder]
}

case class GetTrack(id: TrackID) extends PimpMessage

object GetTrack {
  implicit val json = Json.format[GetTrack]
}

case class Search(term: String, limit: Int) extends PimpMessage

object Search {
  implicit val json = Json.format[Search]
}

case object GetAlarms extends PimpMessage
