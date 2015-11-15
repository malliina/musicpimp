package controllers

import com.mle.musicpimp.audio.TrackMeta
import com.mle.musicpimp.json.JsonMessages
import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.scheduler.json.AlarmJsonHandler
import com.mle.musicpimp.scheduler.web.SchedulerStrings
import com.mle.musicpimp.scheduler.{ClockPlayback, PlaybackJob, ScheduledPlaybackService}
import com.mle.play.Authenticator
import com.mle.util.Log
import play.api.libs.json.Json._
import play.api.libs.json.{JsResult, JsValue, Json, Writes}
import play.api.mvc.Result

class Alarms(auth: Authenticator) extends AlarmEditor(auth) with SchedulerStrings with Log {
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
  val jobWriter = new Writes[PlaybackJob] {
    override def writes(o: PlaybackJob): JsValue = obj(TRACK -> toJson(o.trackInfo))
  }
  implicit val alarmWriter = new Writes[ClockPlayback] {
    override def writes(o: ClockPlayback): JsValue = obj(
      ID -> toJson(o.id),
      JOB -> toJson(o.job)(jobWriter),
      WHEN -> toJson(o.when),
      ENABLED -> toJson(o.enabled)
    )
  }
}
