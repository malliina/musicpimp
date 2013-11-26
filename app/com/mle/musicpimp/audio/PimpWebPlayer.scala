package com.mle.musicpimp.audio

import com.mle.audio.{PlayerStates, StateAwarePlayer, IPlayer}
import scala.concurrent.duration.Duration
import com.mle.musicpimp.library.TrackInfo
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
  with PlaylistSupport[TrackInfo]
  with StateAwarePlayer
  with JsonSender {
  val playlist: BasePlaylist[TrackInfo] = new PimpWebPlaylist(user)
  private val DEFAULT_BROWSER_VOLUME = 100

  // todo update with event listener
  var state: PlayerStates.Value = PlayerStates.Closed
  private var currentVolume: Int = DEFAULT_BROWSER_VOLUME
  private var pos: Duration = Duration.fromNanos(0)
  private var duration: Duration = Duration.fromNanos(0)
  private var isMuted = false

  def setPlaylistAndPlay(song: TrackInfo) {
    playlist set song
    volume = currentVolume
  }

  def trackChanged() {
    val maybeTrack = playlist.current
    duration = maybeTrack.map(_.meta.media.duration) getOrElse Duration.fromNanos(0)
    maybeTrack.map(t => send(JsonMessages.trackChanged(t)))
  }

  def notifyTrackChanged(track: TrackInfo) {
    send(JsonMessages.trackChanged(track))
  }

  def playState: PlayerStates.PlayerState = state

  def playState_=(s: PlayerStates.PlayerState) {
    state = s
    state match {
      case PlayerStates.Started => play()
      case PlayerStates.Stopped => stop()
      case PlayerStates.Closed => send(playStateChanged(PlayState.NoMedia))
      case _ => ()
    }
  }

  def notifyPlayStateChanged(newState: PlayerStates.PlayerState) {
    state = newState
    send(playStateChanged(toPlayState(newState)))
  }

  private def toPlayState(playerState: PlayerStates.PlayerState) = playerState match {
    case PlayerStates.Started => PlayState.Playing
    case PlayerStates.Stopped => PlayState.Stopped
    case PlayerStates.NoMedia => PlayState.NoMedia
    case anythingElse => PlayState.Stopped
  }

  def play() {
    sendCommand(RESUME)
  }

  def notifyPlaying() {
    send(playStateChanged(PlayState.Playing))
  }

  def stop() {
    sendCommand(STOP)
  }

  def notifyStopped() {
    send(playStateChanged(PlayState.Stopped))
  }

  def position = pos

  def position_=(newPos: Duration) {
    pos = newPos
    send(timeUpdated(newPos))
  }

  def seek(pos: Duration) {
    //    send(timeUpdated(pos))
    sendCommand(SEEK, pos.toSeconds)
  }

  def gain = 1.0f * currentVolume / 100

  def gain_=(level: Float) {
    val volumeValue = (level * 100).toInt
    currentVolume = volumeValue
    sendCommand(VOLUME, volumeValue)
  }

  def gain(newGain: Float) {
    gain = newGain
  }

  def notifyVolumeChanged(newVolume: Int) {
    currentVolume = newVolume
    send(volumeChanged(currentVolume))
  }

  def volume = currentVolume

  def volume_=(newVolume: Int) {
    gain = 1.0f * newVolume / 100
  }

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

  def playTrack(song: TrackInfo) {
    duration = song.meta.media.duration
    sendCommand(SKIP, playlist.index)
  }

  def onEndOfMedia() {}

  def status = {
    import StatusEvent._
    val track = playlist.current getOrElse TrackInfo.empty
    toJson(StatusEvent(track, state, pos, currentVolume, isMuted, playlist.songList, playlist.index))
  }

  def statusEvent =
    obj(EVENT -> toJson(STATUS)) ++ status.as[JsObject]

  def statusEvent17 =
    obj(EVENT -> toJson(STATUS)) ++ toJson(status17).as[JsObject]

  def status17 = {
    val track = playlist.current getOrElse TrackInfo.empty
    val tags = track.meta.tags
    StatusEvent17(
      id = track.id,
      title = tags.title,
      artist = tags.artist,
      album = tags.album,
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


