package com.mle.musicpimp.scheduler

import java.nio.file.{Paths, Path}
import play.api.libs.json.Json
import com.mle.util.Log
import com.mle.push.{PushUrl, PushUrls, MPNS}
import play.api.libs.ws.Response
import scala.concurrent.Future
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.concurrent.FutureImplicits._
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.audio.{PlayableTrack, MusicPlayer}
import scala.util.Try
import com.mle.play.json.JsonFormats2

/**
 *
 * @param track the track to play when this job runs
 */
case class PlaybackJob(track: String) extends Job with Log {
  def trackInfo: Option[PlayableTrack] = Library.findMeta(track)

  def describe: String = trackInfo
    .map(t => s"Plays ${t.title}")
    .getOrElse(s"Track not found: $track, so cannot play")

  def toastTo(url: PushUrl): Future[Response] =
    MPNS.toast("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${url.tag}", url.silent)(url.url)

  def toastAndLog(url: PushUrl): Future[Unit] =
    toastTo(url).map(r => {
      log.info(s"Sent toast to: $url. Response: ${r.statusText}.")
      // I believe a 404 suggests that the Microsoft server was reachable but the notification was not delivered.
      // What does this mean? At least, the phone could have turned off push notifications but failed to notify this server.
      // For that reason we may want to remove this push URL as useless; the phone will need to toggle push notifications
      // again to re-enable if needed. What if the 404 is because the phone is just turned off or unreachable?
      //    if (r.status == 404) {
      //      PushUrls.remove(url)
      //    }
    }).recoverAll[Unit](t => log.warn(s"Unable to send toast to: $url", t))

  override def run(): Unit = {
    Try {
      trackInfo.map(t => {
        val initResult = MusicPlayer.tryInitTrackWithFallback(t)
        if (initResult.isSuccess) {
          //        val percentPerSecond = 5
          //        MusicPlayer.volume.foreach(vol => {
          //          val s = Observable.interval(1.second).map(_ + 1).take(100 / percentPerSecond).subscribe(basePercentage => {
          //            MusicPlayer.volume((1.0 * basePercentage * percentPerSecond * 100 * vol).toInt)
          //          })
          //        })
          MusicPlayer.play()
          val toasts = PushUrls.get().map(toastAndLog)
          if (toasts.isEmpty) {
            log.info(s"No push notification URLs are active, so toasting was skipped.")
          }
        }
      }).getOrElse {
        log.warn(s"Unable to find: $track. Cannot start playback.")
      }

    }.recover {
      case t: Throwable => log.warn(s"Failure while running playback job: $describe", t)
    }
  }
}

object PlaybackJob {

  implicit object pathFormat extends JsonFormats2.SimpleFormat[Path](s => Paths.get(s))

  implicit val jsonFormat = Json.format[PlaybackJob]
}