package com.malliina.musicpimp.audio

import java.io.IOException

import com.malliina.musicpimp.audio.TrackPlayer.log
import com.malliina.musicpimp.models.Volume
import play.api.Logger
import rx.lang.scala.Observer

import scala.concurrent.duration.Duration
import scala.util.Try

object TrackPlayer {
  private val log = Logger(getClass)
}

class TrackPlayer(val player: PimpPlayer, events: Observer[ServerMessage]) {
  val track = player.track
  private val stateSubscription = player.events.map(PimpPlayer.playState).subscribe { state =>
    send(PlayStateChangedMessage(state))
  }
  private val timeSubscription = player.timeUpdates.subscribe { time =>
    send(TimeUpdatedMessage(time.position))
  }

  def position = player.position

  def volume = Volume(player.volume)

  def state = player.state

  def volumeCarefully = Try(volume).toOption.orElse(player.cachedVolume.map(Volume.apply))

  def muteCarefully = Try(player.mute).toOption.orElse(player.cachedMute)

  def play(): Unit = player.play()

  def stop(): Unit = {
    player.stop()
    send(PlayStateChangedMessage(Stopped))
  }

  def seek(pos: Duration): Unit =
    trySeek(pos).recover {
      case ioe: IOException if ioe.getMessage == "Resetting to invalid mark" =>
        log.warn(s"Failed to seek to '$pos'. Unable to reset stream.")
    }

  def trySeek(pos: Duration): Try[Unit] = Try {
    if (player.position.toSeconds != pos.toSeconds) {
      player.seek(pos)
      send(TimeUpdatedMessage(pos))
    } else {
      log debug s"Seek to '$pos' refused, already at that position."
    }
  }

  def adjustVolume(level: Volume): Boolean = {
    if (player.volume != level.volume) {
      player.volume = level.volume
      send(VolumeChangedMessage(level))
      true
    } else {
      log debug s"Volume adjustment to '$level' refused, already at that volume."
      false
    }
  }

  def toggleMute(): Unit = mute(!player.mute)

  def mute(shouldMute: Boolean): Unit = {
    if (player.mute != shouldMute) {
      player.mute(shouldMute)
      send(MuteToggledMessage(shouldMute))
    } else {
      log debug s"Unable to set mute to '$shouldMute', already at that state."
    }
  }

  def send(json: ServerMessage): Unit = events.onNext(json)

  def close(): Unit = {
    stateSubscription.unsubscribe()
    timeSubscription.unsubscribe()
    player.close()
    player.media.stream.close()
  }
}
