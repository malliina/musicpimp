package com.mle.musicpimp.audio

import com.mle.audio.{PlayerStates, StateAwarePlayer, IPlayer}
import scala.concurrent.duration.Duration
import com.mle.musicpimp.library.LocalTrack
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonMessages._
import com.mle.musicpimp.json.JsonMessages
import play.api.libs.json.JsObject

/**
 *
 * @author mle
 */
class PimpWebPlayer(val user: String)
  extends IPlayer
  with PlaylistSupport[TrackMeta]
  with StateAwarePlayer
  with JsonSender {
  val playlist: BasePlaylist[TrackMeta] = new PimpWebPlaylist(user)
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
    maybeTrack.map(t => send(JsonMessages.trackChanged(t)))
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

  def play() = sendCommand(RESUME)

  def notifyPlaying() = send(playStateChanged(PlayerStates.Started))

  def stop() = sendCommand(STOP)

  def notifyStopped() = send(playStateChanged(PlayerStates.Stopped))

  def position = pos

  def position_=(newPos: Duration) {
    pos = newPos
    send(timeUpdated(newPos))
  }

  def seek(pos: Duration): Unit = {
    //    send(timeUpdated(pos))
    sendCommand(SEEK, pos.toSeconds)
  }

  def gain = 1.0f * currentVolume / 100

  def gain_=(level: Float) {
    val volumeValue = (level * 100).toInt
    currentVolume = volumeValue
    sendCommand(VOLUME, volumeValue)
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
    sendCommand(MUTE, mute)
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
    sendCommand(MUTE)
  }

  def close() {
    //    send(playStateChanged(PlayState.NoMedia))
    playState = PlayerStates.Closed
  }

  def playTrack(song: TrackMeta) {
    duration = song.duration
    sendCommand(SKIP, playlist.index)
  }

  def status = {
    import StatusEvent._
    val track = playlist.current getOrElse LocalTrack.empty
    toJson(StatusEvent(track, state, pos, currentVolume, isMuted, playlist.songList, playlist.index))
  }

  def statusEvent =
    obj(EVENT -> toJson(STATUS)) ++ status.as[JsObject]

  def statusEvent17 =
    obj(EVENT -> toJson(STATUS)) ++ toJson(status17).as[JsObject]

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


