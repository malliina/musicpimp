package controllers.musicpimp

import javax.sound.sampled.{AudioSystem, LineUnavailableException}

import com.malliina.musicpimp.audio.{MusicPlayer, TrackJson}
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularList, RecentList}
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.models.Username
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{AnyContent, RequestHeader, Result}

import scala.concurrent.Future

class Website(tags: PimpHtml,
              serverWS: ServerWS,
              auth: AuthDeps,
              stats: PlaybackStats)
  extends HtmlController(auth) {

  def player = navigate { req =>
    val hasAudioDevice = AudioSystem.getMixerInfo.nonEmpty
    val feedback: Option[String] =
      if (!hasAudioDevice) {
        Some("Unable to access audio hardware. Playback on this machine is likely to fail.")
      } else {
        MusicPlayer.errorOpt.map(errorMsg)
      }
    val userFeedback = feedback map UserFeedback.error
    tags.basePlayer(userFeedback, req.user)
  }

  def recent = metaAction { (meta, req) =>
    implicit val f = TrackJson.format(req)
    stats.mostRecent(meta) map { entries =>
      default.respond(req)(
        html = tags.mostRecent(entries, meta.username),
        json = RecentList(entries)
      )
    }
  }

  def popular = metaAction { (meta, req) =>
    implicit val f = TrackJson.format(req)
    stats.mostPlayed(meta) map { entries =>
      default.respond(req)(
        html = tags.mostPopular(entries, meta.username),
        json = PopularList(entries)
      )
    }
  }

  protected def metaAction(f: (DataRequest, RequestHeader) => Future[Result]) =
    userAction { req =>
      DataRequest.fromRequest(req).fold(
        error => fut(badRequest(error)),
        meta => f(meta, req)
      )
    }

  protected def userAction(f: AuthenticatedRequest[AnyContent, Username] => Future[Result]) =
    actionAsync(parse.default) { r =>
      f(new AuthenticatedRequest(r.user, r))
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

  def about = navigate(req => tags.aboutBase(req.user))
}
