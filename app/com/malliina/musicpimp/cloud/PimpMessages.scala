package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{FolderID, PlaylistID, RequestID, TrackID}
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.play.ContentRange
import com.malliina.play.models.{Password, Username}
import play.api.libs.json.{JsValue, Json}

object PimpMessages {

  trait PimpMessage

  trait UserMessage extends PimpMessage {
    def username: Username
  }

  trait RequestMessage extends PimpMessage {
    def request: RequestID
  }

  case class RegisteredMessage(id: CloudID) extends PimpMessage

  case object PingMessage extends PimpMessage

  case object PingAuth extends PimpMessage

  case object RootFolder extends PimpMessage

  case class GetFolder(id: FolderID) extends PimpMessage

  case class GetPopular(meta: DataRequest) extends PimpMessage

  case class GetRecent(meta: DataRequest) extends PimpMessage

  case class Track(id: TrackID) extends PimpMessage

  case class CancelStream(request: RequestID) extends RequestMessage

  case class RangedTrack(id: TrackID, range: ContentRange) extends PimpMessage

  case class GetMeta(id: TrackID) extends PimpMessage

  case class Search(term: String, limit: Int) extends PimpMessage

  case object GetAlarms extends PimpMessage

  case class AlarmEdit(payload: JsValue) extends PimpMessage

  case class AlarmAdd(payload: JsValue) extends PimpMessage

  case class PlaybackMessage(payload: JsValue, username: Username) extends UserMessage

  case class Authenticate(username: Username, password: Password) extends UserMessage

  case object GetVersion extends PimpMessage

  case object GetStatus extends PimpMessage

  case class GetPlaylists(username: Username) extends UserMessage

  case class GetPlaylist(id: PlaylistID, username: Username) extends UserMessage

  case class SavePlaylist(playlist: PlaylistSubmission, username: Username) extends UserMessage

  case class DeletePlaylist(id: PlaylistID, username: Username) extends UserMessage

  implicit val searchFormat = Json.format[Search]
  implicit val folderFormat = Json.format[GetFolder]
  implicit val trackFormat = Json.format[Track]
  implicit val authFormat = Json.format[Authenticate]
  implicit val metaFormat = Json.format[GetMeta]
  implicit val registeredFormat = Json.format[RegisteredMessage]
  implicit val rangedFormat = Json.format[RangedTrack]
  implicit val playlistsGet = Json.format[GetPlaylists]
  implicit val playlistGet = Json.format[GetPlaylist]
  implicit val playlistSave = Json.format[SavePlaylist]
  implicit val playlistDelete = Json.format[DeletePlaylist]
  implicit val popularFormat = Json.format[GetPopular]
  implicit val recentFormat = Json.format[GetRecent]
}
