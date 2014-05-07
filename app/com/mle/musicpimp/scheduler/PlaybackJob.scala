package com.mle.musicpimp.scheduler

import java.nio.file.{Path, Paths}
import com.mle.util.Log
import scala.concurrent.Future
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.audio.{PlayableTrack, MusicPlayer}
import scala.util.Try
import com.mle.messaging.gcm.{GcmUrls, GcmUrl, GcmClient}
import com.mle.messaging.mpns.{MPNSClient, PushUrls, PushUrl}
import com.mle.play.json.JsonFormats2
import play.api.libs.json.Json
import com.mle.concurrent.FutureImplicits.RichFuture
import com.ning.http.client.Response

/**
 *
 * @param track the track to play when this job runs
 */
case class PlaybackJob(track: String) extends Job with Log {
  def trackInfo: Option[PlayableTrack] = Library.findMeta(track)

  def describe: String = trackInfo.fold(s"Track not found: $track, so cannot play")(t => s"Plays ${t.title}")

  def sendMPNS(url: PushUrl): Future[Unit] = MPNSClient.sendLogged(url)

  def sendGcm(url: GcmUrl): Future[Unit] = GcmClient.sendLogged(url)

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
          val toasts = PushUrls.get().map(MPNSClient.sendLogged)
          val gcms = GcmUrls.get().map(GcmClient.sendLogged)
          val messages = toasts ++ gcms
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

  implicit object pathFormat extends JsonFormats2.SimpleFormat[Path](s => Paths.get(s))

  implicit val jsonFormat = Json.format[PlaybackJob]
}