package com.malliina.musicpimp.scheduler

import java.nio.file.{Path, Paths}

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.audio.{MusicPlayer, PlayableTrack}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.messaging.TokenService
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler.PlaybackJob.log
import play.api.Logger
import play.api.libs.json.Json

/**
  * @param track the track to play when this job runs
  */
case class PlaybackJob(track: TrackID) extends Job {
  def trackInfo: Option[PlayableTrack] = Library.findMeta(track)

  def describe: String = trackInfo.fold(s"Track not found: $track, so cannot play")(t => s"Plays ${t.title}")

  override def run(): Unit = {
    trackInfo.fold(log.warn(s"Unable to find: $track. Cannot start playback.")) { t =>
      MusicPlayer.setPlaylistAndPlay(t).map { _ =>
        TokenService.default.sendNotifications()
      }.recover {
        case t: Throwable => log.warn(s"Failure while running playback job: $describe", t)
      }
    }
  }
}

object PlaybackJob {
  private val log = Logger(getClass)

  implicit object pathFormat extends JsonFormats.SimpleFormat[Path](s => Paths.get(s))

  implicit val jsonFormat = Json.format[PlaybackJob]
}
