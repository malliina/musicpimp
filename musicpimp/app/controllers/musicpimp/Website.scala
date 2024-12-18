package controllers.musicpimp

import cats.effect.IO
import com.malliina.musicpimp.audio.{MusicPlayer, TrackJson}
import com.malliina.musicpimp.html.PimpHtml
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.stats.{DataRequest, PlaybackStats, PopularList, RecentList}
import com.malliina.play.tags.TagPage
import com.malliina.values.Username

import javax.sound.sampled.{AudioSystem, LineUnavailableException}
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.{AnyContent, RequestHeader, Result}

class Website(
  musicPlayer: MusicPlayer,
  tags: PimpHtml,
  serverWS: ServerWS,
  auth: AuthDeps,
  stats: PlaybackStats[IO]
) extends HtmlController(auth):

  def player = navigate[TagPage]: req =>
    val hasAudioDevice = AudioSystem.getMixerInfo.nonEmpty
    val feedback: Option[String] =
      if !hasAudioDevice then
        Some("Unable to access audio hardware. Playback on this machine is likely to fail.")
      else musicPlayer.errorOpt.map(errorMsg)
    val userFeedback = feedback.map(UserFeedback.error)
    tags.basePlayer(userFeedback, req.user)

  def recent = metaAction: (meta, req) =>
    stats
      .mostRecent(meta)
      .map: entries =>
        val list = RecentList.forEntries(meta, entries, TrackJson.host(req))
        default.respond(req)(
          html = tags.mostRecent(list),
          json = list
        )

  def popular = metaAction: (meta, req) =>
    stats
      .mostPlayed(meta)
      .map: entries =>
        val list = PopularList.forEntries(meta, entries, TrackJson.host(req))
        default.respond(req)(
          html = tags.mostPopular(list),
          json = list
        )

  private def metaAction(f: (DataRequest, RequestHeader) => IO[Result]) =
    userAction: req =>
      DataRequest
        .fromRequest(req)
        .fold(
          error => IO.pure(badRequest(error)),
          meta => f(meta, req)
        )

  private def userAction(f: AuthenticatedRequest[AnyContent, Username] => IO[Result]) =
    actionAsyncIO(comps.parsers.default): r =>
      f(new AuthenticatedRequest(r.user, r))

  def errorMsg(t: Throwable): String = t match
    case _: LineUnavailableException =>
      "Playback could not be started. To troubleshoot this issue, you may wish to verify that audio " +
        "playback is possible on the server and that the audio drivers are working. Check the sound " +
        "properties of your Java Virtual Machine. If you use OpenJDK, you may want to try Oracle's JVM " +
        "instead and vice versa. The playback exception is a LineUnavailableException."
    case t: Throwable =>
      val msg = Option(t.getMessage) getOrElse ""
      s"Playback could not be started. $msg"

  def about = navigate(req => tags.aboutBase(req.user))
