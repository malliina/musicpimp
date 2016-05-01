package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates

import scala.concurrent.duration.FiniteDuration

trait PlayerMessage

case class TimeUpdated(position: FiniteDuration) extends PlayerMessage

case class TrackChanged(track: String) extends PlayerMessage

case class VolumeChanged(volume: Int) extends PlayerMessage

case class PlaylistIndexChanged(index: Int) extends PlayerMessage

case class PlayStateChanged(state: PlayerStates.Value) extends PlayerMessage

case class MuteToggled(isMute: Boolean) extends PlayerMessage

case class Play(track: String) extends PlayerMessage

case class Add(track: String) extends PlayerMessage

case class Remove(index: Int) extends PlayerMessage

case object Resume extends PlayerMessage

case object Stop extends PlayerMessage

case object Next extends PlayerMessage

case object Prev extends PlayerMessage

case class Skip(index: Int) extends PlayerMessage

case class Seek(position: FiniteDuration) extends PlayerMessage

case class Mute(isMute: Boolean) extends PlayerMessage

case class Volume(volume: Int) extends PlayerMessage

case class InsertTrack(index: Int, track: String) extends PlayerMessage

case class MoveTrack(from: Int, to: Int) extends PlayerMessage

case class ResetPlaylistMessage(index: Int, tracks: Seq[String]) extends PlayerMessage

case class PlayAll(folders: Seq[String], tracks: Seq[String]) extends PlayerMessage

case class AddAll(folders: Seq[String], tracks: Seq[String]) extends PlayerMessage