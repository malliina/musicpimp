package com.malliina.musicpimp.audio

import com.malliina.audio.IPlayer
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.Target
import com.malliina.musicpimp.library.LocalTrack
import com.malliina.musicpimp.models.{RemoteInfo, Volume}
import play.api.libs.json.JsObject
import play.api.libs.json.Json._

import scala.concurrent.duration.Duration
import scala.util.{Success, Try}

class PimpWebPlayer(val request: RemoteInfo, val target: Target)
  extends IPlayer
    with PlaylistSupport[TrackMeta]
    with JsonSender {

  val user = request.user
  implicit val trackWriter = TrackJson.format(request.host)
  val playlist: BasePlaylist[TrackMeta] = new PimpWebPlaylist(user, target)
  private val DEFAULT_BROWSER_VOLUME = Volume(100)

  // todo update with event listener
  var state: PlayState = Closed
  private var currentVolume: Volume = DEFAULT_BROWSER_VOLUME
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
    maybeTrack.foreach(notifyTrackChanged)
  }

  def notifyTrackChanged(track: TrackMeta) {
    sendPayload(TrackChangedMessage(track))
  }

  def notifyStateChanged(state: PlayState) =
    sendPayload(PlayStateChangedMessage(state))

  def playState: PlayState = state

  def playState_=(s: PlayState) {
    state = s
    state match {
      case Started => play()
      case Stopped => stop()
      case Closed => notifyStateChanged(NoMedia)
      case _ => ()
    }
  }

  def notifyPlayStateChanged(newState: PlayState) {
    state = newState
    notifyStateChanged(newState)
  }

  def play() = sendCommand(Resume)

  def notifyPlaying() = notifyStateChanged(Started)

  def stop() = sendCommand(Stop)

  def notifyStopped() = notifyStateChanged(Stopped)

  def position = pos

  def position_=(newPos: Duration) {
    pos = newPos
    sendPayload(TimeUpdatedMessage(newPos))
  }

  def seek(pos: Duration): Unit = {
    sendCommand(Seek, pos.toSeconds)
  }

  def gain = 1.0f * currentVolume.volume / 100

  def gain_=(level: Float) {
    val volumeValue = Volume((level * 100).toInt)
    currentVolume = volumeValue
    sendCommand(VolumeKey, volumeValue)
  }

  def gain(newGain: Float): Unit = gain = newGain

  def notifyVolumeChanged(newVolume: Volume) {
    currentVolume = newVolume
    sendPayload(VolumeChangedMessage(currentVolume))
  }

  def volume = currentVolume

  def volume_=(newVolume: Volume): Unit = gain = 1.0f * newVolume.volume / 100

  def volume(newVolume: Int): Unit = volume = Volume(newVolume)

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
    sendPayload(MuteToggledMessage(newMute))
  }

  def toggleMute() {
    isMuted = !isMuted
    sendCommand(Mute)
  }

  def close(): Unit = {
    playState = Closed
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
    obj(EventKey -> toJson(StatusKey)) ++ status.as[JsObject]

  def statusEvent17 =
    obj(EventKey -> toJson(StatusKey)) ++ toJson(status17)(StatusEvent17.status17writer).as[JsObject]

  def status17 = {
    val track = playlist.current getOrElse LocalTrack.empty
    StatusEvent17(
      id = track.id,
      title = track.title,
      artist = track.artist,
      album = track.album,
      state = playState,
      position = pos,
      duration = duration,
      gain = 1.0f * currentVolume.volume / 100,
      mute = isMuted,
      playlist = playlist.songList,
      index = playlist.index
    )
  }
}
