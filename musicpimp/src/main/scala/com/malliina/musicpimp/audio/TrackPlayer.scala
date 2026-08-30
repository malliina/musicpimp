package com.malliina.musicpimp.audio

import cats.effect.{Async, Temporal}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.audio.TrackPlayer.log
import com.malliina.musicpimp.models.Volume
import com.malliina.util.AppLogger
import fs2.concurrent.Topic

import java.io.IOException
import scala.concurrent.duration.Duration
import scala.util.Try

object TrackPlayer:
  private val log = AppLogger(getClass)

class TrackPlayer[F[_]: Async](
  val player: PimpPlayer[F],
  serverMessages: Topic[F, ServerMessage]
):
  val F = Async[F]
  val track = player.track

  def position = player.position
  def volume = Volume(player.volume)
  def state = player.state
  def volumeCarefully = Try(volume).toOption.orElse(player.cachedVolume.map(Volume.apply))
  def muteCarefully = Try(player.mute).toOption.orElse(player.cachedMute)
  def play(): F[Unit] = player.play()

  def stop(): F[Unit] =
    player.stop() >> send(PlayStateChangedMessage(Stopped))

  def seek(pos: Duration): F[Unit] =
    trySeek(pos).handleError:
      case ioe: IOException if ioe.getMessage == "Resetting to invalid mark" =>
        log.warn(s"Failed to seek to '$pos'. Unable to reset stream.")
      case e =>
        log.warn(s"Failed to seek to '$pos'.", e)

  def trySeek(pos: Duration): F[Unit] =
    if player.position.toSeconds != pos.toSeconds then
      player.seek(pos) >>
        send(TimeUpdatedMessage(pos))
    else F.delay(log.debug(s"Seek to '$pos' refused, already at that position."))

  def adjustVolume(level: Volume): F[Boolean] =
    if player.volume != level.volume then
      player.volume = level.volume
      send(VolumeChangedMessage(level)).as(true)
    else
      F.delay(log.debug(s"Volume adjustment to '$level' refused, already at that volume."))
        .as(false)

  def toggleMute(): F[Unit] = mute(!player.mute)

  def mute(shouldMute: Boolean): F[Unit] =
    if player.mute != shouldMute then
      player.mute(shouldMute) >>
        send(MuteToggledMessage(shouldMute))
    else F.delay(log.debug(s"Unable to set mute to '$shouldMute', already at that state."))

  def send(json: ServerMessage): F[Unit] = serverMessages.publish1(json).void

  def close(): Unit =
    player.close()
    player.media.stream.close()
