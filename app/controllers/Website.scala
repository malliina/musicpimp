package controllers

import views.html
import com.mle.util.Log
import javax.sound.sampled.{LineUnavailableException, AudioSystem}
import com.mle.musicpimp.library.Settings
import com.mle.musicpimp.audio.MusicPlayer
import rx.lang.scala.Observable
import com.mle.logbackrx.LogEvent
import play.api.mvc.Call
import com.mle.musicpimp.log.RxLogbackAppender

/**
 *
 * @author mle
 */
object Website
  extends Secured
  with HtmlController
  with SettingsController
  with ConnectController
  with LibraryController
  with PimpAccountController
  with StreamingLogController
  with Logs
  with Log {

  override def subscribeCall: Call = routes.Website.openLogSocket

  override def logEvents: Observable[LogEvent] = RxLogbackAppender.logEvents

  def player = navigate(implicit req => {
    val hasAudioDevice = AudioSystem.getMixerInfo.size > 0
    val feedback: Option[String] =
      if (!hasAudioDevice) {
        Some("Unable to access audio hardware. Playback on this machine is likely to fail.")
      } else {
        MusicPlayer.errorOpt.map(errorMsg)
      }
    html.player(feedback)
  })

  def errorMsg(t: Throwable): String = t match {
    case _: LineUnavailableException =>
      "Playback could not be started. To troubleshoot this issue, you may wish to verify that audio " +
        "playback is possible on the server and that the audio drivers are working. Check the sound " +
        "properties of your Java Virtual Machine. If you use OpenJDK, you may want to try Oracle's JVM " +
        "instead and vice versa. The playback exception is a LineUnavailableException."
    case t: Throwable =>
      val msg = Option(t.getMessage) getOrElse ""
      s"Playback could not be started. $msg"
  }

  def popupPlayer = navigate(implicit req => html.popupPlayer())

  def manage = navigate(html.musicFolders(Settings.readFolders, newFolderForm))

  def about = navigate(html.aboutBase())

  def parameters = navigate(html.parameters(Rest.uploadDir.toAbsolutePath.toString))
}