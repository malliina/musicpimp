package com.malliina.musicpimp.scheduler

import java.nio.file.{Path, Paths}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.json.JsonFormats
import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.library.MusicLibrary
import com.malliina.musicpimp.messaging.TokenService
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler.PlaybackJob.log
import play.api.Logger

/**
  * @param trackId the track to play when this job runs
  */
case class PlaybackJob(
  id: Option[String],
  when: ClockSchedule,
  trackId: TrackID,
  player: MusicPlayer,
  lib: MusicLibrary
) extends Job {
  def describe: String = s"Plays $trackId"

  override def run(): Unit =
    lib
      .meta(trackId)
      .map { maybeTrack =>
        maybeTrack.map { track =>
          player.setPlaylistAndPlay(track).map { _ =>
            TokenService.default.sendNotifications()
          }
        }.getOrElse {
          log.error(s"Track not found: '$trackId'.")
        }
      }
      .recover {
        case t: Exception => log.warn(s"Failure while running playback job: $describe", t)
      }
}

object PlaybackJob {
  def apply(conf: ClockPlaybackConf, player: MusicPlayer, lib: MusicLibrary): PlaybackJob =
    PlaybackJob(conf.id, conf.when, conf.track, player, lib)

  private val log = Logger(getClass)

  implicit object pathFormat extends JsonFormats.SimpleFormat[Path](s => Paths.get(s))
}
