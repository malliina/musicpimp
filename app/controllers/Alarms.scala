package controllers

import akka.stream.Materializer
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.scheduler.json.AlarmJsonHandler
import com.malliina.musicpimp.scheduler.web.SchedulerStrings
import com.malliina.musicpimp.scheduler.{ClockPlayback, PlaybackJob, ScheduledPlaybackService}
import com.malliina.play.Authenticator
import controllers.Alarms.log
import play.api.Logger
import play.api.i18n.Messages
import play.api.libs.json.Json._
import play.api.libs.json.{JsResult, JsValue, Json, Writes}
import play.api.mvc.Result

class Alarms(auth: Authenticator, messages: Messages, mat: Materializer)
  extends AlarmEditor(auth, messages, mat)
    with SchedulerStrings {

  def alarms = PimpAction(implicit request => {
    def content: Seq[ClockPlayback] = ScheduledPlaybackService.status
    respond(
      html = views.html.alarms(content),
      json = Json.toJson(content)
    )
  })

  def handleJson = PimpParsedAction(parse.json)(jsonRequest => {
    val json = jsonRequest.body
    log debug s"User: ${jsonRequest.user} from: ${jsonRequest.remoteAddress} said: $json"
    onRequest(json)
  })

  def tracks = PimpAction(implicit request => {
    val tracks: Iterable[TrackMeta] = Library.tracksRecursive
    implicit val w = TrackMeta.writer(request)
    Ok(Json.toJson(tracks))
  })

  def paths = PimpAction(implicit request => {
    val tracks = Library.songPathsRecursive
    implicit val pathFormat = PlaybackJob.pathFormat
    Ok(Json.toJson(tracks))
  })

  private def onRequest(json: JsValue): Result = {
    val jsResult = AlarmJsonHandler.handle(json)
    simpleResult(json, jsResult)
  }

  private def simpleResult[T](json: JsValue, result: JsResult[T]): Result = {
    result.fold(
      errors => BadRequest(JsonMessages.failure(s"Invalid JSON: $json. Errors: $errors.")),
      valid => Ok)
  }
}

object Alarms {
  private val log = Logger(getClass)

  def jobWriter(implicit w: Writes[TrackMeta]) = Writes[PlaybackJob](o => obj(TrackKey -> toJson(o.trackInfo)))

  implicit def alarmWriter(implicit w: Writes[TrackMeta]) = Writes[ClockPlayback](o => obj(
    Id -> toJson(o.id),
    Job -> toJson(o.job)(jobWriter),
    When -> toJson(o.when),
    Enabled -> toJson(o.enabled)
  ))
}
