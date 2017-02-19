package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.models.{FolderID, TrackID}

import scala.concurrent.duration.FiniteDuration

trait PlayerMessage

case class TimeUpdatedMsg(position: FiniteDuration) extends PlayerMessage

case class TrackChangedMsg(track: TrackID) extends PlayerMessage

case class VolumeChangedMsg(volume: Int) extends PlayerMessage

case class PlaylistIndexChangedMsg(index: Int) extends PlayerMessage

case class PlayStateChangedMsg(state: PlayerStates.Value) extends PlayerMessage

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