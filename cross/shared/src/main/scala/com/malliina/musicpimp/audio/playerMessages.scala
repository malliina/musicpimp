package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats.{cmd, finiteDuration, singleCmd}
import com.malliina.musicpimp.json.PlaybackStrings._
import com.malliina.musicpimp.models.{FolderID, TrackID, Volume}
import play.api.libs.json._

import scala.concurrent.duration.FiniteDuration

case object ToggleMuteMessage {
  val Key = "mute"
  implicit val json: OFormat[ToggleMuteMessage.type] = singleCmd(Key, ToggleMuteMessage)
}

case object GetStatusMsg extends PlayerMessage {
  val Key = "status"
  implicit val json: OFormat[GetStatusMsg.type] = singleCmd(Key, GetStatusMsg)
}

/** Web playback updates only.
  *
  * @param value pos
  */
case class TimeUpdatedMsg(value: FiniteDuration) extends PlayerMessage

object TimeUpdatedMsg {
  implicit val json: OFormat[TimeUpdatedMsg] = cmd(TimeUpdated, Json.format[TimeUpdatedMsg])
}

case class TrackChangedMsg(track: TrackID) extends PlayerMessage

object TrackChangedMsg {
  implicit val json: OFormat[TrackChangedMsg] = cmd(TrackChanged, Json.format[TrackChangedMsg])
}

case class VolumeChangedMsg(value: Volume) extends PlayerMessage

object VolumeChangedMsg {
  implicit val json: OFormat[VolumeChangedMsg] = cmd(VolumeChanged, Json.format[VolumeChangedMsg])
}

case class PlaylistIndexChangedMsg(value: Int) extends PlayerMessage

object PlaylistIndexChangedMsg {
  implicit val json: OFormat[PlaylistIndexChangedMsg] =
    cmd(PlaylistIndexChanged, Json.format[PlaylistIndexChangedMsg])
}

case class PlayStateChangedMsg(value: PlayState) extends PlayerMessage

object PlayStateChangedMsg {
  implicit val json: OFormat[PlayStateChangedMsg] =
    cmd(PlaystateChanged, Json.format[PlayStateChangedMsg])
}

case class MuteToggledMsg(value: Boolean) extends PlayerMessage

object MuteToggledMsg {
  implicit val json: OFormat[MuteToggledMsg] = cmd(MuteToggled, Json.format[MuteToggledMsg])
}

case class PlayMsg(track: TrackID) extends PlayerMessage

object PlayMsg {
  implicit val json: OFormat[PlayMsg] = cmd(Play, Json.format[PlayMsg])
}

case class AddMsg(track: TrackID) extends PlayerMessage

object AddMsg {
  implicit val json: OFormat[AddMsg] = cmd(Add, Json.format[AddMsg])
}

case class RemoveMsg(index: Int) extends PlayerMessage

object RemoveMsg {
  val Index = "index"
  val reader = Reads[RemoveMsg] { json =>
    (json \ Value).validate[Int].orElse((json \ Index).validate[Int]).map(apply)
  }
  val writer = OWrites[RemoveMsg] { r => Json.obj(Value -> r.index, Index -> r.index) }
  implicit val json: OFormat[RemoveMsg] = cmd(Remove, OFormat(reader, writer))
}

case object ResumeMsg extends PlayerMessage {
  implicit val json: OFormat[ResumeMsg.type] = singleCmd(Resume, ResumeMsg)
}

case object StopMsg extends PlayerMessage {
  implicit val json: OFormat[StopMsg.type] = singleCmd(Stop, StopMsg)
}

case object NextMsg extends PlayerMessage {
  implicit val json: OFormat[NextMsg.type] = singleCmd(Next, NextMsg)
}

case object PrevMsg extends PlayerMessage {
  implicit val json: OFormat[PrevMsg.type] = singleCmd(Prev, PrevMsg)
}

case class SkipMsg(value: Int) extends PlayerMessage

object SkipMsg {
  implicit val json: OFormat[SkipMsg] = cmd(Skip, Json.format[SkipMsg])
}

case class SeekMsg(value: FiniteDuration) extends PlayerMessage

object SeekMsg {
  implicit val json: OFormat[SeekMsg] = cmd(Seek, Json.format[SeekMsg])
}

case class MuteMsg(value: Boolean) extends PlayerMessage

object MuteMsg {
  implicit val json: OFormat[MuteMsg] = cmd(Mute, Json.format[MuteMsg])
}

case class VolumeMsg(value: Volume) extends PlayerMessage

object VolumeMsg {
  implicit val json: OFormat[VolumeMsg] = cmd(VolumeKey, Json.format[VolumeMsg])
}

case class InsertTrackMsg(index: Int, track: TrackID) extends PlayerMessage

object InsertTrackMsg {
  implicit val json: OFormat[InsertTrackMsg] = cmd(Insert, Json.format[InsertTrackMsg])
}

case class MoveTrackMsg(from: Int, to: Int) extends PlayerMessage

object MoveTrackMsg {
  implicit val json: OFormat[MoveTrackMsg] = cmd(Move, Json.format[MoveTrackMsg])
}

case class ResetPlaylistMessage(index: Int, tracks: Seq[TrackID]) extends PlayerMessage

object ResetPlaylistMessage {
  val reader = Reads[ResetPlaylistMessage] { json =>
    val idx = (json \ Index).validate[Int].getOrElse(-1)
    val tracks = (json \ Tracks).validate[Seq[TrackID]].getOrElse(Nil)
    JsSuccess(ResetPlaylistMessage(idx, tracks))
  }
  implicit val json: OFormat[ResetPlaylistMessage] =
    cmd(ResetPlaylist, OFormat(reader, Json.writes[ResetPlaylistMessage]))
}

case class Handover(
  index: Option[Int],
  tracks: Seq[TrackID],
  state: PlayState,
  position: FiniteDuration
) extends PlayerMessage

object Handover {
  val Key = "handover"
  implicit val json: OFormat[Handover] = cmd(Key, Json.format[Handover])
}

case class PlayAllMsg(tracks: Seq[TrackID], folders: Seq[FolderID]) extends PlayerMessage

object PlayAllMsg {
  implicit val json: OFormat[PlayAllMsg] =
    cmd(PlayItemsKey, OFormat(ItemsLike.reader[PlayAllMsg](apply), Json.writes[PlayAllMsg]))
}

case class AddAllMsg(tracks: Seq[TrackID], folders: Seq[FolderID]) extends PlayerMessage

object AddAllMsg {
  implicit val json: OFormat[AddAllMsg] =
    cmd(AddItemsKey, OFormat(ItemsLike.reader[AddAllMsg](apply), Json.writes[AddAllMsg]))
}

/** Two uses:
  *
  * Messages sent from clients to MusicPimp servers to control server playback.
  *
  * Messages sent from web players to MusicPimp to sync web player state with the server. (legacy)
  *
  * @see PlaybackMessageHandler
  * @see WebPlayerMessageHandler
  */
trait PlayerMessage

object PlayerMessage {
  implicit val reader: Reads[PlayerMessage] = Reads { json =>
    GetStatusMsg.json
      .reads(json)
      .orElse(TimeUpdatedMsg.json.reads(json))
      .orElse(TrackChangedMsg.json.reads(json))
      .orElse(json.validate[VolumeChangedMsg])
      .orElse(json.validate[PlaylistIndexChangedMsg])
      .orElse(json.validate[PlayStateChangedMsg])
      .orElse(json.validate[MuteToggledMsg])
      .orElse(json.validate[PlayMsg])
      .orElse(json.validate[AddMsg])
      .orElse(json.validate[RemoveMsg])
      .orElse(ResumeMsg.json.reads(json))
      .orElse(StopMsg.json.reads(json))
      .orElse(NextMsg.json.reads(json))
      .orElse(PrevMsg.json.reads(json))
      .orElse(json.validate[SkipMsg])
      .orElse(json.validate[SeekMsg])
      .orElse(json.validate[MuteMsg])
      .orElse(json.validate[VolumeMsg])
      .orElse(json.validate[InsertTrackMsg])
      .orElse(json.validate[MoveTrackMsg])
      .orElse(json.validate[ResetPlaylistMessage])
      .orElse(json.validate[PlayAllMsg])
      .orElse(json.validate[AddAllMsg])
      .orElse(json.validate[Handover])
  }
}

object ItemsLike {
  def reader[T](build: (Seq[TrackID], Seq[FolderID]) => T): Reads[T] = Reads[T] { json =>
    val folders = (json \ Folders).validate[Seq[FolderID]].getOrElse(Nil)
    val tracks = (json \ Tracks).validate[Seq[TrackID]].getOrElse(Nil)
    JsSuccess(build(tracks, folders))
  }
}

case object StatusMsg {
  implicit val json: OFormat[StatusMsg.type] = singleCmd(Status, StatusMsg)
}
