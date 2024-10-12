package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.audio.PlayerMessage
import com.malliina.musicpimp.json.CrossFormats.singleCmd
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{CloudID, PlaylistID, RequestID, TrackID}
import com.malliina.musicpimp.scheduler.json.AlarmCommand
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.play.ContentRange
import com.malliina.values.Username
import io.circe.generic.semiauto.deriveDecoder
import io.circe.{Codec, Decoder, Encoder}

trait UserMessage extends PimpMessage:
  def username: Username

trait RequestMessage extends PimpMessage:
  def request: RequestID

case class RegisteredMessage(id: CloudID) extends PimpMessage derives Codec.AsObject

case class RangedTrack(id: TrackID, range: ContentRange) extends PimpMessage derives Codec.AsObject

case class GetPlaylists(username: Username) extends UserMessage derives Codec.AsObject

case class GetPlaylist(id: PlaylistID, username: Username) extends UserMessage
  derives Codec.AsObject

case class SavePlaylist(playlist: PlaylistSubmission, username: Username) extends UserMessage
  derives Codec.AsObject

case class DeletePlaylist(id: PlaylistID, username: Username) extends UserMessage
  derives Codec.AsObject

case class GetPopular(meta: DataRequest) extends PimpMessage derives Codec.AsObject

case class GetRecent(meta: DataRequest) extends PimpMessage derives Codec.AsObject

case class CancelStream(request: RequestID) extends RequestMessage

case class AlarmEdit(body: AlarmCommand) extends PimpMessage

case class AlarmAdd(body: AlarmCommand) extends PimpMessage

case class PlaybackMessage(body: PlayerMessage, username: Username) extends UserMessage

object PlaybackMessage:
  given Decoder[PlaybackMessage] = deriveDecoder

case object GetVersion extends PimpMessage:
  val Key = "version"
  implicit val json: Codec[GetVersion.type] = singleCmd(Key, GetVersion)

case object GetStatus extends PimpMessage:
  val Key = "status"
  implicit val json: Codec[GetStatus.type] = singleCmd(Key, GetStatus)
