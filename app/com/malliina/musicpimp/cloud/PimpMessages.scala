package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{RequestID, PlaylistID, User}
import com.malliina.play.ContentRange
import play.api.libs.json.{JsValue, Json}

object PimpMessages {

  trait PimpMessage

  trait RequestMessage extends PimpMessage {
    def request: RequestID
  }

  case class Registered(id: CloudID) extends PimpMessage

  case object Ping extends PimpMessage

  case object PingAuth extends PimpMessage

  case object RootFolder extends PimpMessage

  case class Folder(id: String) extends PimpMessage

  case class Track(id: String) extends PimpMessage

  case class RangedTrack(id: String, range: ContentRange) extends PimpMessage

  case class GetMeta(id: String) extends PimpMessage

  case class Search(term: String, limit: Int) extends PimpMessage

  case object GetAlarms extends PimpMessage

  case class AlarmEdit(payload: JsValue) extends PimpMessage

  case class AlarmAdd(payload: JsValue) extends PimpMessage

  case class PlaybackMessage(payload: JsValue) extends PimpMessage

  case class Authenticate(username: User, password: String) extends PimpMessage

  case object GetVersion extends PimpMessage

  case object GetStatus extends PimpMessage

  case class GetPlaylists(username: User) extends PimpMessage

  case class GetPlaylist(id: PlaylistID, username: User) extends PimpMessage

  case class SavePlaylist(playlist: PlaylistSubmission, username: User) extends PimpMessage

  case class DeletePlaylist(id: PlaylistID, username: User) extends PimpMessage

  implicit val searchFormat = Json.format[Search]
  implicit val folderFormat = Json.format[Folder]
  implicit val trackFormat = Json.format[Track]
  implicit val authFormat = Json.format[Authenticate]
  implicit val meta = Json.format[GetMeta]
  implicit val registeredFormat = Json.format[Registered]
  implicit val rangedFormat = Json.format[RangedTrack]
  implicit val playlistsGet = Json.format[GetPlaylists]
  implicit val playlistGet = Json.format[GetPlaylist]
  implicit val playlistSave = Json.format[SavePlaylist]
  implicit val playlistDelete = Json.format[DeletePlaylist]
}
