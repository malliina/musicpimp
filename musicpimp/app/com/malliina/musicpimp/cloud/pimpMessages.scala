package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.audio.PlayerMessage
import com.malliina.musicpimp.json.CrossFormats.singleCmd
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{CloudID, PlaylistID, RequestID, TrackID}
import com.malliina.musicpimp.scheduler.json.AlarmCommand
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.play.ContentRange
import com.malliina.play.models.Username
import play.api.libs.json.{JsValue, Json}

trait UserMessage extends PimpMessage {
  def username: Username
}

trait RequestMessage extends PimpMessage {
  def request: RequestID
}

case class RegisteredMessage(id: CloudID) extends PimpMessage

object RegisteredMessage {
  implicit val json = Json.format[RegisteredMessage]
}

case class RangedTrack(id: TrackID, range: ContentRange) extends PimpMessage

object RangedTrack {
  implicit val json = Json.format[RangedTrack]
}

case class GetPlaylists(username: Username) extends UserMessage

object GetPlaylists {
  implicit val json = Json.format[GetPlaylists]
}

case class GetPlaylist(id: PlaylistID, username: Username) extends UserMessage

object GetPlaylist {
  implicit val json = Json.format[GetPlaylist]
}

case class SavePlaylist(playlist: PlaylistSubmission, username: Username) extends UserMessage

object SavePlaylist {
  implicit val json = Json.format[SavePlaylist]
}

case class DeletePlaylist(id: PlaylistID, username: Username) extends UserMessage

object DeletePlaylist {
  implicit val json = Json.format[DeletePlaylist]
}

case class GetPopular(meta: DataRequest) extends PimpMessage

object GetPopular {
  implicit val json = Json.format[GetPopular]
}

case class GetRecent(meta: DataRequest) extends PimpMessage

object GetRecent {
  implicit val json = Json.format[GetRecent]
}

case class CancelStream(request: RequestID) extends RequestMessage

case class AlarmEdit(body: AlarmCommand) extends PimpMessage

case class AlarmAdd(body: AlarmCommand) extends PimpMessage

case class PlaybackMessage(body: PlayerMessage, username: Username) extends UserMessage

object PlaybackMessage {
  implicit val reader = Json.reads[PlaybackMessage]
}

case object GetVersion extends PimpMessage {
  val Key = "version"
  implicit val json = singleCmd(Key, GetVersion)
}

case object GetStatus extends PimpMessage {
  val Key = "status"
  implicit val json = singleCmd(Key, GetStatus)
}
