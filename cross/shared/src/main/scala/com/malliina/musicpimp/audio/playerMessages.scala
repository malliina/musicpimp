package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats.{cmd, finiteDuration, singleCmd}
import com.malliina.musicpimp.json.PlaybackStrings.*
import com.malliina.musicpimp.models.{FolderID, TrackID, Volume}
import io.circe.generic.semiauto.{deriveCodec, deriveEncoder}
import io.circe.{Codec, Decoder, Encoder, Json}
import io.circe.syntax.EncoderOps
import scala.concurrent.duration.FiniteDuration

case object ToggleMuteMessage:
  val Key = "mute"
  implicit val json: Codec[ToggleMuteMessage.type] = singleCmd(Key, ToggleMuteMessage)

case object GetStatusMsg extends PlayerMessage:
  val Key = "status"
  implicit val json: Codec[GetStatusMsg.type] = singleCmd(Key, GetStatusMsg)

/** Web playback updates only.
  *
  * @param value
  *   pos
  */
case class TimeUpdatedMsg(value: FiniteDuration) extends PlayerMessage

object TimeUpdatedMsg:
  implicit val json: Codec[TimeUpdatedMsg] = cmd(TimeUpdated, deriveCodec[TimeUpdatedMsg])

case class TrackChangedMsg(track: TrackID) extends PlayerMessage

object TrackChangedMsg:
  implicit val json: Codec[TrackChangedMsg] = cmd(TrackChanged, deriveCodec[TrackChangedMsg])

case class VolumeChangedMsg(value: Volume) extends PlayerMessage

object VolumeChangedMsg:
  implicit val json: Codec[VolumeChangedMsg] = cmd(VolumeChanged, deriveCodec[VolumeChangedMsg])

case class PlaylistIndexChangedMsg(value: Int) extends PlayerMessage

object PlaylistIndexChangedMsg:
  implicit val json: Codec[PlaylistIndexChangedMsg] =
    cmd(PlaylistIndexChanged, deriveCodec[PlaylistIndexChangedMsg])

case class PlayStateChangedMsg(value: PlayState) extends PlayerMessage

object PlayStateChangedMsg:
  implicit val json: Codec[PlayStateChangedMsg] =
    cmd(PlaystateChanged, deriveCodec[PlayStateChangedMsg])

case class MuteToggledMsg(value: Boolean) extends PlayerMessage

object MuteToggledMsg:
  implicit val json: Codec[MuteToggledMsg] = cmd(MuteToggled, deriveCodec[MuteToggledMsg])

case class PlayMsg(track: TrackID) extends PlayerMessage

object PlayMsg:
  implicit val json: Codec[PlayMsg] = cmd(Play, deriveCodec[PlayMsg])

case class AddMsg(track: TrackID) extends PlayerMessage

object AddMsg:
  implicit val json: Codec[AddMsg] = cmd(Add, deriveCodec[AddMsg])

case class RemoveMsg(index: Int) extends PlayerMessage

object RemoveMsg:
  val Index = "index"
  val reader: Decoder[RemoveMsg] = json =>
    json.downField(Value).as[Int].orElse(json.downField(Index).as[Int]).map(apply)
  val writer: Encoder[RemoveMsg] = r => Json.obj(Value -> r.index.asJson, Index -> r.index.asJson)
  given json: Codec[RemoveMsg] = cmd(Remove, Codec.from(reader, writer))

case object ResumeMsg extends PlayerMessage:
  given json: Codec[ResumeMsg.type] = singleCmd(Resume, ResumeMsg)

case object StopMsg extends PlayerMessage:
  given json: Codec[StopMsg.type] = singleCmd(Stop, StopMsg)

case object NextMsg extends PlayerMessage:
  given json: Codec[NextMsg.type] = singleCmd(Next, NextMsg)

case object PrevMsg extends PlayerMessage:
  given json: Codec[PrevMsg.type] = singleCmd(Prev, PrevMsg)

case class SkipMsg(value: Int) extends PlayerMessage

object SkipMsg:
  given json: Codec[SkipMsg] = cmd(Skip, deriveCodec[SkipMsg])

case class SeekMsg(value: FiniteDuration) extends PlayerMessage

object SeekMsg:
  given json: Codec[SeekMsg] = cmd(Seek, deriveCodec[SeekMsg])

case class MuteMsg(value: Boolean) extends PlayerMessage

object MuteMsg:
  given json: Codec[MuteMsg] = cmd(Mute, deriveCodec[MuteMsg])

case class VolumeMsg(value: Volume) extends PlayerMessage

object VolumeMsg:
  given json: Codec[VolumeMsg] = cmd(VolumeKey, deriveCodec[VolumeMsg])

case class InsertTrackMsg(index: Int, track: TrackID) extends PlayerMessage

object InsertTrackMsg:
  given json: Codec[InsertTrackMsg] = cmd(Insert, deriveCodec[InsertTrackMsg])

case class MoveTrackMsg(from: Int, to: Int) extends PlayerMessage

object MoveTrackMsg:
  given json: Codec[MoveTrackMsg] = cmd(Move, deriveCodec[MoveTrackMsg])

case class ResetPlaylistMessage(index: Int, tracks: Seq[TrackID]) extends PlayerMessage

object ResetPlaylistMessage:
  val reader: Decoder[ResetPlaylistMessage] = json =>
    val idx = json.downField(Index).as[Int].getOrElse(-1)
    val tracks = json.downField(Tracks).as[Seq[TrackID]].getOrElse(Nil)
    Right(ResetPlaylistMessage(idx, tracks))
  given json: Codec[ResetPlaylistMessage] =
    cmd(ResetPlaylist, Codec.from(reader, deriveEncoder[ResetPlaylistMessage]))

case class Handover(
  index: Option[Int],
  tracks: Seq[TrackID],
  state: PlayState,
  position: FiniteDuration
) extends PlayerMessage

object Handover:
  val Key = "handover"
  given json: Codec[Handover] = cmd(Key, deriveCodec[Handover])

case class PlayAllMsg(tracks: Seq[TrackID], folders: Seq[FolderID]) extends PlayerMessage

object PlayAllMsg:
  given json: Codec[PlayAllMsg] =
    cmd(PlayItemsKey, Codec.from(ItemsLike.reader[PlayAllMsg](apply), deriveEncoder[PlayAllMsg]))

case class AddAllMsg(tracks: Seq[TrackID], folders: Seq[FolderID]) extends PlayerMessage

object AddAllMsg:
  given json: Codec[AddAllMsg] =
    cmd(AddItemsKey, Codec.from(ItemsLike.reader[AddAllMsg](apply), deriveEncoder[AddAllMsg]))

/** Two uses:
  *
  * Messages sent from clients to MusicPimp servers to control server playback.
  *
  * Messages sent from web players to MusicPimp to sync web player state with the server. (legacy)
  *
  * @see
  *   PlaybackMessageHandler
  * @see
  *   WebPlayerMessageHandler
  */
trait PlayerMessage

object PlayerMessage:
  implicit val reader: Decoder[PlayerMessage] = json =>
    GetStatusMsg.json
      .decodeJson(json.value)
      .orElse(TimeUpdatedMsg.json.decodeJson(json.value))
      .orElse(TrackChangedMsg.json.decodeJson(json.value))
      .orElse(json.as[VolumeChangedMsg])
      .orElse(json.as[PlaylistIndexChangedMsg])
      .orElse(json.as[PlayStateChangedMsg])
      .orElse(json.as[MuteToggledMsg])
      .orElse(json.as[PlayMsg])
      .orElse(json.as[AddMsg])
      .orElse(json.as[RemoveMsg])
      .orElse(ResumeMsg.json.decodeJson(json.value))
      .orElse(StopMsg.json.decodeJson(json.value))
      .orElse(NextMsg.json.decodeJson(json.value))
      .orElse(PrevMsg.json.decodeJson(json.value))
      .orElse(json.as[SkipMsg])
      .orElse(json.as[SeekMsg])
      .orElse(json.as[MuteMsg])
      .orElse(json.as[VolumeMsg])
      .orElse(json.as[InsertTrackMsg])
      .orElse(json.as[MoveTrackMsg])
      .orElse(json.as[ResetPlaylistMessage])
      .orElse(json.as[PlayAllMsg])
      .orElse(json.as[AddAllMsg])
      .orElse(json.as[Handover])

object ItemsLike:
  def reader[T](build: (Seq[TrackID], Seq[FolderID]) => T): Decoder[T] = json =>
    val folders = json.downField(Folders).as[Seq[FolderID]].getOrElse(Nil)
    val tracks = json.downField(Tracks).as[Seq[TrackID]].getOrElse(Nil)
    Right(build(tracks, folders))

case object StatusMsg:
  given json: Codec[StatusMsg.type] = singleCmd(Status, StatusMsg)
