package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.{FolderID, TrackID}

import scala.concurrent.duration.FiniteDuration

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

/** Web playback updates only.
  *
  * @param position pos
  */
case class TimeUpdatedMsg(position: FiniteDuration) extends PlayerMessage

case class TrackChangedMsg(track: TrackID) extends PlayerMessage

case class VolumeChangedMsg(volume: Int) extends PlayerMessage

case class PlaylistIndexChangedMsg(index: Int) extends PlayerMessage

case class PlayStateChangedMsg(state: PlayState) extends PlayerMessage

case class MuteToggledMsg(isMute: Boolean) extends PlayerMessage

case class PlayMsg(track: TrackID) extends PlayerMessage

case class AddMsg(track: TrackID) extends PlayerMessage

case class RemoveMsg(index: Int) extends PlayerMessage

case object ResumeMsg extends PlayerMessage

case object StopMsg extends PlayerMessage

case object NextMsg extends PlayerMessage

case object PrevMsg extends PlayerMessage

case class SkipMsg(index: Int) extends PlayerMessage

case class SeekMsg(position: FiniteDuration) extends PlayerMessage

case class MuteMsg(isMute: Boolean) extends PlayerMessage

case class VolumeMsg(volume: Int) extends PlayerMessage

case class InsertTrackMsg(index: Int, track: TrackID) extends PlayerMessage

case class MoveTrackMsg(from: Int, to: Int) extends PlayerMessage

case class ResetPlaylistMessage(index: Int, tracks: Seq[TrackID]) extends PlayerMessage

case class PlayAllMsg(folders: Seq[FolderID], tracks: Seq[TrackID]) extends PlayerMessage

case class AddAllMsg(folders: Seq[FolderID], tracks: Seq[TrackID]) extends PlayerMessage
