package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.values.{Password, Username}
import play.api.libs.json.{Json, OFormat}

trait PimpMessage

case object PingMessage extends PimpMessage
case object PongMessage extends PimpMessage
case object PingAuth extends PimpMessage
case object RootFolder extends PimpMessage

case class GetFolder(id: FolderID) extends PimpMessage

object GetFolder {
  implicit val json: OFormat[GetFolder] = Json.format[GetFolder]
}

case class GetTrack(id: TrackID) extends PimpMessage

object GetTrack {
  implicit val json: OFormat[GetTrack] = Json.format[GetTrack]
}

case class Search(term: String, limit: Int) extends PimpMessage

object Search {
  implicit val json: OFormat[Search] = Json.format[Search]
}

case object GetAlarms extends PimpMessage

case class Authenticate(username: Username, password: Password) extends PimpMessage

object Authenticate {
  implicit val json: OFormat[Authenticate] = Json.format[Authenticate]
}

case class GetMeta(id: TrackID) extends PimpMessage

object GetMeta {
  implicit val json: OFormat[GetMeta] = Json.format[GetMeta]
}
