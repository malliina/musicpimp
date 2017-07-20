package com.malliina.musicpimp.scheduler

import java.nio.file.{Path, Paths}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.json.JsonFormats
import com.malliina.musicpimp.audio.{MusicPlayer, PlayableTrack}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.messaging.adm.{AmazonDevices, CloudAdmClient}
import com.malliina.musicpimp.messaging.apns.{APNSDevices, CloudAPNSClient}
import com.malliina.musicpimp.messaging.gcm.{CloudGcmClient, GoogleDevices}
import com.malliina.musicpimp.messaging.mpns.{CloudMicrosoftClient, PushUrls}
import com.malliina.musicpimp.models.TrackID
import com.malliina.musicpimp.scheduler.PlaybackJob.log
import play.api.Logger
import play.api.libs.json.Json

import scala.concurrent.Future

/**
  * @param track the track to play when this job runs
  */
case class PlaybackJob(track: TrackID) extends Job {
  def trackInfo: Option[PlayableTrack] = Library.findMeta(track)

  def describe: String = trackInfo.fold(s"Track not found: $track, so cannot play")(t => s"Plays ${t.title}")

  override def run(): Unit = {
    trackInfo.fold(log.warn(s"Unable to find: $track. Cannot start playback.")) { t =>
      MusicPlayer.setPlaylistAndPlay(t).map { _ =>
        val apns = APNSDevices.get().map(CloudAPNSClient.sendLogged)
        val toasts = PushUrls.get().map(CloudMicrosoftClient.sendLogged)
        val gcms = GoogleDevices.get().map(CloudGcmClient.sendLogged)
        val adms = AmazonDevices.get().map(CloudAdmClient.sendLogged)
        val messages = apns ++ toasts ++ gcms ++ adms
        if (messages.isEmpty) {
          log.info(s"No push notification URLs are active, so no push notifications were sent.")
        } else {
          Future.sequence(messages)
            .map(seq => log info s"Sent ${seq.size} notifications.")
            .recoverAll(t => log.warn(s"Unable to send all notifications. ${t.getClass.getName}", t))
        }
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
