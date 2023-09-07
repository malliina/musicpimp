package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.audio.PlayerMessage
import com.malliina.musicpimp.json.CrossFormats.singleCmd
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{CloudID, PlaylistID, RequestID, TrackID}
import com.malliina.musicpimp.scheduler.json.AlarmCommand
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.play.ContentRange
import com.malliina.values.Username
import play.api.libs.json.{Json, OFormat, Reads}

trait UserMessage extends PimpMessage {
  def username: Username
}

trait RequestMessage extends PimpMessage {
  def request: RequestID
}

case class RegisteredMessage(id: CloudID) extends PimpMessage

object RegisteredMessage {
  implicit val json: OFormat[RegisteredMessage] = Json.format[RegisteredMessage]
}

case class RangedTrack(id: TrackID, range: ContentRange) extends PimpMessage

object RangedTrack {
  implicit val json: OFormat[RangedTrack] = Json.format[RangedTrack]
}

case class GetPlaylists(username: Username) extends UserMessage

object GetPlaylists {
  implicit val json: OFormat[GetPlaylists] = Json.format[GetPlaylists]
}

case class GetPlaylist(id: PlaylistID, username: Username) extends UserMessage

object GetPlaylist {
  implicit val json: OFormat[GetPlaylist] = Json.format[GetPlaylist]
}

case class SavePlaylist(playlist: PlaylistSubmission, username: Username) extends UserMessage

object SavePlaylist {
  implicit val json: OFormat[SavePlaylist] = Json.format[SavePlaylist]
}

case class DeletePlaylist(id: PlaylistID, username: Username) extends UserMessage

object DeletePlaylist {
  implicit val json: OFormat[DeletePlaylist] = Json.format[DeletePlaylist]
}

case class GetPopular(meta: DataRequest) extends PimpMessage

object GetPopular {
  implicit val json: OFormat[GetPopular] = Json.format[GetPopular]
}

case class GetRecent(meta: DataRequest) extends PimpMessage

object GetRecent {
  implicit val json: OFormat[GetRecent] = Json.format[GetRecent]
}

case class CancelStream(request: RequestID) extends RequestMessage

case class AlarmEdit(body: AlarmCommand) extends PimpMessage

case class AlarmAdd(body: AlarmCommand) extends PimpMessage

case class PlaybackMessage(body: PlayerMessage, username: Username) extends UserMessage

object PlaybackMessage {
  implicit val reader: Reads[PlaybackMessage] = Json.reads[PlaybackMessage]
}

case object GetVersion extends PimpMessage {
  val Key = "version"
  implicit val json: OFormat[GetVersion.type] = singleCmd(Key, GetVersion)
}

case object GetStatus extends PimpMessage {
  val Key = "status"
  implicit val json: OFormat[GetStatus.type] = singleCmd(Key, GetStatus)
}
