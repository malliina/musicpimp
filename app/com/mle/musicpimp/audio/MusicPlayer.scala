package com.mle.musicpimp.audio

import com.mle.audio._
import com.mle.musicpimp.actor.ServerPlayerManager
import com.mle.actor.Messages.Stop
import com.mle.musicpimp.actor.Messages.Restart
import com.mle.util.{Utils, Util, Log}
import scala.Some
import scala.concurrent.duration.Duration
import com.mle.musicpimp.library.TrackInfo
import com.mle.musicpimp.json.{JsonSendah, JsonMessages}
import scala.util.{Failure, Try}

/**
 * @author Michael
 */
object MusicPlayer
  extends IPlayer
  with PlaylistSupport[TrackInfo]
  with JsonSendah
  with Log {

  private val defaultVolume = 40
  val playlist: PimpPlaylist = new PimpPlaylist

  var errorOpt: Option[Throwable] = None

  /**
   * Every time the track changes, a new [[PimpJavaSoundPlayer]] is used.
   * - Well, why?
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
    val previousVolume: Option[Int] = Try(player.map(_.volume)).toOption.flatten
    //      Utils.opt[Option[Int], IllegalArgumentException](player.map(_.volume)).flatten
    val previousMute: Option[Boolean] = Try(player.map(_.mute)).toOption.flatten
    close()
    log.info(s"Closed track, now initializing: ${track.title}")
    // PimpJavaSoundPlayer.ctor throws at least LineUnavailableException if the audio device cannot be initialized
    val newPlayer = new PimpJavaSoundPlayer(track) {
      override def onEndOfMedia(): Unit = {
        log.info(s"End of media for ${track.title}")
        nextTrack()
      }
    }
    player = Some(newPlayer)

    // Maintains the gain & mute status as they were in the previous track.
    // If there was no previous gain, there was no previous track,
    // so we set the default volume.
    val volumeChanged = setVolume(previousVolume getOrElse defaultVolume)
    // ensures the volume message is always sent
    if (!volumeChanged) {
      sendCurrentVolume()
    }
    previousMute foreach mute

    send(JsonMessages.trackChanged(track))
  }

  def play() {
    player.foreach(p => {
      p.play()
      send(JsonMessages.playStateChanged(PlayerStates.Started))
      ServerPlayerManager.playbackPoller ! Restart
    })
  }

  def stop() {
    player.foreach(p => {
      p.stop()
      send(JsonMessages.playStateChanged(PlayerStates.Stopped)) //PlayState.Stopped))
    })
    ServerPlayerManager.playbackPoller ! Stop
  }

  def seek(pos: Duration) {
    player.filter(_.position.toSeconds != pos.toSeconds).foreach(p => {
      p.seek(pos)
      send(JsonMessages.timeUpdated(pos))
    })
  }

  def volume(level: Int): Unit = setVolume(level)

  /**
   *
   * @param level new volume
   * @return true if the volume was changed, false otherwise
   */
  def setVolume(level: Int): Boolean =
    player.filter(_.volume != level).map(p => {
      p.volume = level
      sendVolumeChanged(level)
    }).isDefined

  def sendVolumeChanged(level: Int) = send(JsonMessages.volumeChanged(level))

  def sendCurrentVolume() = sendVolumeChanged(player.map(_.volume) getOrElse 0)

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

  def close(): Unit = player.foreach(p => p.close())

  def position =
    player.map(_.position).getOrElse {
      log.info(s"Unable to obtain position because no player is initialized, defaulting to 0.")
      Duration.fromNanos(0)
    }

  def status: StatusEvent = player.map(p => StatusEvent(
    p.track,
    p.state,
    p.position,
    p.volume,
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
        gain = 1.0f * p.volume / 100,
        mute = p.mute,
        playlist = playlist.songList,
        index = playlist.index
      )
    }).getOrElse(StatusEvent17.noServerTrackEvent)
  }
}

