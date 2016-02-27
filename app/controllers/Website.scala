package controllers

import javax.sound.sampled.{AudioSystem, LineUnavailableException}

import com.malliina.musicpimp.audio.MusicPlayer
import com.malliina.musicpimp.models.User
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.play.Authenticator
import com.malliina.play.ws.WebSocketController
import play.api.http.Writeable
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.AnyContent
import play.api.mvc.Security.AuthenticatedRequest
import views.html

import scala.concurrent.Future

class Website(sockets: WebSocketController, serverWS: ServerWS, auth: Authenticator, stats: PlaybackStats)
  extends HtmlController(auth) {

  def player = navigate(implicit req => {
    val hasAudioDevice = AudioSystem.getMixerInfo.nonEmpty
    val feedback: Option[String] =
      if (!hasAudioDevice) {
        Some("Unable to access audio hardware. Playback on this machine is likely to fail.")
      } else {
        MusicPlayer.errorOpt.map(errorMsg)
      }
    html.player(serverWS.wsUrl, feedback)
  })

  def recent = userAction { req =>
    val user = req.user
    stats.mostRecent(user, count = 100)
      .map(entries => html.mostRecent(entries, user))
  }

  def popular = userAction { req =>
    val user = req.user
    stats.mostPlayed(user)
      .map(entries => html.mostPopular(entries, user))
  }

  protected def userAction[R: Writeable](f: AuthenticatedRequest[AnyContent, User] => Future[R]) =
    pimpActionAsync2 { r =>
      f(new AuthenticatedRequest(User(r.user), r))
    }

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

  def popupPlayer = navigate(implicit req => html.popupPlayer(sockets.wsUrl))

  def about = navigate(html.aboutBase())

  def parameters = navigate(html.parameters())
}
