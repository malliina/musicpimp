package com.malliina.musicpimp.scheduler

import cats.effect.kernel.Sync
import cats.effect.std.Dispatcher
import cats.implicits.{catsSyntaxApplicativeError, toFunctorOps}
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.messaging.TokenService
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler.PlaybackJob.log
import com.malliina.util.AppLogger

/** @param trackId
  *   the track to play when this job runs
  */
case class PlaybackJob[F[_]: Sync](
  id: Option[String],
  when: ClockSchedule,
  trackId: TrackID,
  player: MusicPlayer[F],
  lib: MusicLibrary[F],
  tokenService: TokenService[F],
  dispatcher: Dispatcher[F]
) extends Job:
  def describe: String = s"Plays $trackId"

  override def run(): Unit =
    dispatcher.unsafeRunAndForget(task)

  def task =
    lib
      .meta(trackId)
      .map: maybeTrack =>
        maybeTrack
          .map: track =>
            player
              .setPlaylistAndPlay(track)
              .map: _ =>
                tokenService.sendNotifications()
          .getOrElse:
            log.error(s"Track not found: '$trackId'.")
      .handleError:
        case t: Exception => log.warn(s"Failure while running playback job: $describe", t)

object PlaybackJob:
  private val log = AppLogger(getClass)

  def apply[F[_]: Sync](
    conf: ClockPlaybackConf,
    player: MusicPlayer[F],
    lib: MusicLibrary[F],
    tokenService: TokenService[F],
    d: Dispatcher[F]
  ): PlaybackJob[F] =
    PlaybackJob(conf.id, conf.when, conf.track, player, lib, tokenService, d)
