package com.malliina.musicpimp.scheduler

import java.nio.file.{Path, Paths}

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.concurrent.FutureOps
import com.malliina.musicpimp.audio.{MusicPlayer, PlayableTrack}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.messaging.AndroidDevice
import com.malliina.musicpimp.messaging.adm.{AdmClient, AmazonDevices}
import com.malliina.musicpimp.messaging.gcm.{GCMDevice, GcmClient, GoogleDevices}
import com.malliina.musicpimp.messaging.mpns.{MicrosoftClient, PushUrls}
import com.malliina.play.json.JsonFormats
import com.malliina.push.mpns.PushUrl
import com.malliina.util.Log
import play.api.libs.json.Json

import scala.concurrent.Future
import scala.util.Try

/**
 *
 * @param track the track to play when this job runs
 */
case class PlaybackJob(track: String) extends Job with Log {
  def trackInfo: Option[PlayableTrack] = Library.findMeta(track)

  def describe: String = trackInfo.fold(s"Track not found: $track, so cannot play")(t => s"Plays ${t.title}")

  def sendMPNS(url: PushUrl): Future[Unit] = MicrosoftClient.sendLogged(url)

  def sendGcm(url: GCMDevice): Future[Unit] = GcmClient.sendLogged(url)

  override def run(): Unit = {
    Try {
      trackInfo.fold(log.warn(s"Unable to find: $track. Cannot start playback."))(t => {
        val initResult = MusicPlayer.tryInitTrackWithFallback(t)
        if (initResult.isSuccess) {
          //        val percentPerSecond = 5
          //        MusicPlayer.volume.foreach(vol => {
          //          val s = Observable.interval(1.second).map(_ + 1).take(100 / percentPerSecond).subscribe(basePercentage => {
          //            MusicPlayer.volume((1.0 * basePercentage * percentPerSecond * 100 * vol).toInt)
          //          })
          //        })
          MusicPlayer.play()
          val toasts = PushUrls.get().map(MicrosoftClient.sendLogged)
          val gcms = GoogleDevices.get().map(GcmClient.sendLogged)
          val adms = AmazonDevices.get().map(AdmClient.sendLogged)
          val messages = toasts ++ gcms ++ adms
          if (messages.isEmpty) {
            log.info(s"No push notification URLs are active, so no push notifications were sent.")
          }
          Future.sequence(messages)
            .map(seq => log info s"Sent ${seq.size} messages.")
            .recoverAll(t => log.warn(s"Unable to send all messages. ${t.getClass.getName}", t))
        }
      })

    }.recover {
      case t: Throwable => log.warn(s"Failure while running playback job: $describe", t)
    }
  }
}

object PlaybackJob {

  implicit object pathFormat extends JsonFormats.SimpleFormat[Path](s => Paths.get(s))

  implicit val jsonFormat = Json.format[PlaybackJob]
}