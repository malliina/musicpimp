package com.mle.musicpimp.audio

import com.mle.audio._
import com.mle.musicpimp.actor.ServerPlayerManager
import com.mle.actor.Messages.Stop
import com.mle.musicpimp.actor.Messages.Restart
import com.mle.util.Log
import scala.Some
import scala.concurrent.duration.Duration
import com.mle.musicpimp.library.TrackInfo
import com.mle.musicpimp.json.{JsonSendah, JsonMessages}
import scala.util.{Failure, Try}
import akka.actor.Status.Success
import javax.sound.sampled.LineUnavailableException

/**
 * @author Michael
 */
object MusicPlayer
  extends IPlayer
  with PlaylistSupport[TrackInfo]
  with JsonSendah
  with Log {

  val playlist: PimpPlaylist = new PimpPlaylist

  var errorOpt: Option[Throwable] = None

  /**
   * Every time the track changes, a new [[PimpJavaSoundPlayer]] is used.
   */
  private var player: Option[PimpJavaSoundPlayer] = None

  def underLying = player

  def reset(track: TrackInfo): Unit = {
    playlist set track
    playlist.current.foreach(playTrack)

  }

  def playTrack(songMeta: TrackInfo): Unit = {
    errorOpt = None
    Try(initTrack(songMeta)) match {
      case Failure(t) =>
        log.warn(s"Unable to play track ${songMeta.id}", t)
        errorOpt = Some(t)
      case _ =>
        play()
    }
  }

  /**
   *
   * @param track
   * @throws LineUnavailableException
   */
  private def initTrack(track: TrackInfo) {
    throw new LineUnavailableException("test")
    val previousGain = player.map(_.gain)
    val previousMute = player.map(_.mute)
    close()
    val newPlayer = new PimpJavaSoundPlayer(track) {
      override def onEndOfMedia(): Unit = nextTrack()
    }
    // PimpJavaSoundPlayer.ctor throws at least LineUnavailableException if the audio device cannot be initialized
    player = Some(newPlayer)
    // Maintains the gain & mute status as they were in the previous track.
    // If there was no previous gain, there was no previous track,
    // so we send the current volume as an initial value.
    previousGain map gain getOrElse sendCurrentVolume()
    previousMute foreach mute

    send(JsonMessages.trackChanged(track))
  }

  def play() {
    player.foreach(p => {
      p.play()
      send(JsonMessages.playStateChanged(PlayState.Playing))
      ServerPlayerManager.playbackPoller ! Restart
    })
  }

  def stop() {
    player.foreach(p => {
      p.stop()
      send(JsonMessages.playStateChanged(PlayState.Stopped))
    })
    ServerPlayerManager.playbackPoller ! Stop
  }

  def seek(pos: Duration) {
    player.filter(_.position.toSeconds != pos.toSeconds).foreach(p => {
      p.seek(pos)
      send(JsonMessages.timeUpdated(pos))
    })
  }

  def gain(level: Float) {
    player.filter(_.gain != level).foreach(p => {
      p.gain(level)
      sendVolumeChanged(level)
    })
  }

  def sendVolumeChanged(level: Float) {
    send(JsonMessages.volumeChanged((level * 100).toInt))
  }

  def sendCurrentVolume() {
    sendVolumeChanged(player.map(_.gain) getOrElse 0f)
  }

  def mute(mute: Boolean) {
    player.filter(_.mute != mute).foreach(p => {
      p.mute(mute)
      send(JsonMessages.muteToggled(mute))
    })
  }

  def toggleMute() {
    player.foreach(p => {
      p.toggleMute()
      send(JsonMessages.muteToggled(p.mute))
    })
  }

  def close() {
    player.foreach(_.close())
  }

  def position = player.map(_.position) getOrElse Duration.fromNanos(0)

  def status: StatusEvent = player.map(p => StatusEvent(
    p.track,
    p.state,
    p.position,
    (p.gain * 100).toInt,
    p.mute,
    playlist.songList,
    playlist.index
  )).getOrElse(StatusEvent.empty)

  def status17: StatusEvent17 = {
    player.map(p => {
      val meta = p.meta
      StatusEvent17(
        id = p.track.id,
        title = meta.tags.title,
        artist = meta.tags.artist,
        album = meta.tags.album,
        state = p.state,
        position = p.position,
        duration = p.duration,
        gain = p.gain,
        mute = p.mute,
        playlist = playlist.songList,
        index = playlist.index
      )
    }).getOrElse(StatusEvent17.noServerTrackEvent)
  }
}

