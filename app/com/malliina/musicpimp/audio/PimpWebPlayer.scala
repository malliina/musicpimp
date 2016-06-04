package com.malliina.musicpimp.audio

import com.malliina.audio.{IPlayer, PlayerStates, StateAwarePlayer}
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonMessages._
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.LocalTrack
import com.malliina.musicpimp.models.RemoteInfo
import controllers.WebPlayer
import play.api.libs.json.JsObject
import play.api.libs.json.Json._

import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

class PimpWebPlayer(val request: RemoteInfo, val webPlayer: WebPlayer)
  extends IPlayer
  with PlaylistSupport[TrackMeta]
  with StateAwarePlayer
  with JsonSender {

  val user = request.user
  implicit val trackWriter = TrackMeta.writer(request.host)
  val playlist: BasePlaylist[TrackMeta] = new PimpWebPlaylist(user, webPlayer)
  private val DEFAULT_BROWSER_VOLUME = 100

  // todo update with event listener
  var state: PlayerStates.Value = PlayerStates.Closed
  private var currentVolume: Int = DEFAULT_BROWSER_VOLUME
  private var pos: Duration = Duration.fromNanos(0)
  private var duration: Duration = Duration.fromNanos(0)
  private var isMuted = false

  def setPlaylistAndPlay(song: TrackMeta) {
    playlist set song
    volume = currentVolume
  }

  def trackChanged() {
    val maybeTrack = playlist.current
    duration = maybeTrack.map(_.duration) getOrElse Duration.fromNanos(0)
    maybeTrack.foreach(t => send(JsonMessages.trackChanged(t)))
  }

  def notifyTrackChanged(track: TrackMeta) {
    send(JsonMessages.trackChanged(track))
  }

  def playState: PlayerStates.PlayerState = state

  def playState_=(s: PlayerStates.PlayerState) {
    state = s
    state match {
      case PlayerStates.Started => play()
      case PlayerStates.Stopped => stop()
      case PlayerStates.Closed => send(playStateChanged(PlayerStates.NoMedia))
      case _ => ()
    }
  }

  def notifyPlayStateChanged(newState: PlayerStates.PlayerState) {
    state = newState
    send(playStateChanged(newState))
  }

  def play() = sendCommand(Resume)

  def notifyPlaying() = send(playStateChanged(PlayerStates.Started))

  def stop() = sendCommand(Stop)

  def notifyStopped() = send(playStateChanged(PlayerStates.Stopped))

  def position = pos

  def position_=(newPos: Duration) {
    pos = newPos
    send(timeUpdated(newPos))
  }

  def seek(pos: Duration): Unit = {
    //    send(timeUpdated(pos))
    sendCommand(Seek, pos.toSeconds)
  }

  def gain = 1.0f * currentVolume / 100

  def gain_=(level: Float) {
    val volumeValue = (level * 100).toInt
    currentVolume = volumeValue
    sendCommand(Volume, volumeValue)
  }

  def gain(newGain: Float): Unit = gain = newGain

  def notifyVolumeChanged(newVolume: Int) {
    currentVolume = newVolume
    send(volumeChanged(currentVolume))
  }

  def volume = currentVolume

  def volume_=(newVolume: Int): Unit = gain = 1.0f * newVolume / 100

  def volume(newVolume: Int): Unit = volume = newVolume

  def mute = isMuted

  def mute_=(mute: Boolean) {
    isMuted = mute
    sendCommand(Mute, mute)
  }

  def mute(newMute: Boolean) {
    mute = newMute
  }

  def notifyMuteToggled(newMute: Boolean) {
    isMuted = newMute
    send(muteToggled(newMute))
  }

  def toggleMute() {
    isMuted = !isMuted
    sendCommand(Mute)
  }

  def close() {
    //    send(playStateChanged(PlayState.NoMedia))
    playState = PlayerStates.Closed
  }

  def playTrack(song: TrackMeta): Try[Unit] = {
    duration = song.duration
    sendCommand(Skip, playlist.index)
    Success(())
  }

  def status = {
    val track = playlist.current getOrElse LocalTrack.empty
    val event = StatusEvent(
      track, state, pos,
      currentVolume, isMuted, playlist.songList,
      playlist.index)
    toJson(event)(StatusEvent.status18writer)
  }

  def statusEvent =
    obj(Event -> toJson(Status)) ++ status.as[JsObject]

  def statusEvent17 =
    obj(Event -> toJson(Status)) ++ toJson(status17)(StatusEvent17.status17writer).as[JsObject]

  def status17 = {
    val track = playlist.current getOrElse LocalTrack.empty
    StatusEvent17(
      id = track.id,
      title = track.title,
      artist = track.artist,
      album = track.album,
      state = state,
      position = pos,
      duration = duration,
      gain = 1.0f * currentVolume / 100,
      mute = isMuted,
      playlist = playlist.songList,
      index = playlist.index
    )
  }
}


