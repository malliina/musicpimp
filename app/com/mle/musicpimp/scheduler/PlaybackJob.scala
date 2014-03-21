package com.mle.musicpimp.scheduler

import java.nio.file.{Paths, Path}
import play.api.libs.json.Json
import com.mle.util.Log
import com.mle.push.{PushUrl, PushUrls, MPNS}
import play.api.libs.ws.Response
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import com.mle.concurrent.FutureImplicits._
import com.mle.musicpimp.library.{TrackInfo, Library}
import com.mle.musicpimp.audio.MusicPlayer
import com.mle.musicpimp.json.SimpleFormat

/**
 *
 * @param track the track to play when this job runs
 */
case class PlaybackJob(track: String) extends Job with Log {
  val trackInfo = Library.meta(track)
  def describe: String = s"Plays ${trackInfo.title}"

  def toastTo(url: PushUrl): Future[Response] =
    MPNS.toast("MusicPimp", "Tap to stop", s"/MusicPimp/Xaml/AlarmClock.xaml?DeepLink=true&cmd=stop&tag=${url.tag}", url.silent)(url.url)

  def toastAndLog(url: PushUrl) = toastTo(url)
    .map(r => log.info(s"Sent toast to: $url. Response: ${r.statusText}."))
    .recoverAll[Unit](t => log.warn(s"Unable to send toast to: $url", t))

  override def run(): Unit = {
    try {
      MusicPlayer.playTrack(trackInfo)
      val toasts = PushUrls.get().map(toastAndLog)
      if (toasts.isEmpty) {
        log.info(s"No push notification URLs are active, so toasting was skipped.")
      }
    } catch {
      case t: Throwable => log.warn(s"Cockup", t)
    }
  }
}

object PlaybackJob {

  implicit object pathFormat extends SimpleFormat[Path](s => Paths.get(s))

  implicit val jsonFormat = Json.format[PlaybackJob]
}