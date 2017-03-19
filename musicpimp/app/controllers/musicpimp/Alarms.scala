package controllers.musicpimp

import com.malliina.musicpimp.audio.{TrackJson, TrackMeta}
import com.malliina.musicpimp.http.PimpContentController.default
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.{ClockPlayback, PlaybackJob, ScheduledPlaybackService}
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.play.http.Proxies
import controllers.musicpimp.Alarms.log
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json._
import play.api.libs.json.{JsResult, JsValue, Json, Writes}
import play.api.mvc.Result

class Alarms(tags: PimpHtml,
             auth: AuthDeps,
             messages: Messages)
  extends AlarmEditor(tags, auth, messages)
    with SchedulerStrings {

  def alarms = pimpAction { request =>
    def content: Seq[ClockPlayback] = ScheduledPlaybackService.status

    default.respond(request)(
      html = tags.alarms(content, request.user),
      json = Json.toJson(content)
    )
  }

  def handleJson = pimpParsedAction(parse.json) { jsonRequest =>
    val json = jsonRequest.body
    val remoteAddress = Proxies.realAddress(jsonRequest)
    log debug s"User '${jsonRequest.user}' from '$remoteAddress' said '$json'."
    onRequest(json)
  }

  def tracks = pimpAction { request =>
    val tracks: Iterable[TrackMeta] = Library.tracksRecursive
    implicit val w = TrackJson.writer(request)
    Ok(Json.toJson(tracks))
  }

  def paths = pimpAction { request =>
    val tracks = Library.songPathsRecursive
    implicit val pathFormat = PlaybackJob.pathFormat
    Ok(Json.toJson(tracks))
  }

  private def onRequest(json: JsValue): Result = {
    val jsResult = AlarmJsonHandler.handle(json)
    simpleResult(json, jsResult)
  }

  private def simpleResult[T](json: JsValue, result: JsResult[T]): Result = {
    result.fold(
      errors => badRequest(s"Invalid JSON '$json'. Errors '$errors'."),
      _ => Ok
    )
  }
}

object Alarms {
  private val log = Logger(getClass)

  def jobWriter(implicit w: Writes[TrackMeta]) =
    Writes[PlaybackJob](o => obj(TrackKey -> toJson(o.trackInfo)))

  implicit def alarmWriter(implicit w: Writes[TrackMeta]) =
    Writes[ClockPlayback](o => obj(
      Id -> toJson(o.id),
      Job -> toJson(o.job)(jobWriter),
      When -> toJson(o.when),
      Enabled -> toJson(o.enabled)
    ))
}
