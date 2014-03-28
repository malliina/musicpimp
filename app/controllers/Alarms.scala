package controllers

import play.api.libs.json.{Writes, JsResult, JsValue, Json}
import com.mle.util.Log
import com.mle.musicpimp.scheduler.json.JsonHandler
import com.mle.musicpimp.scheduler.web.SchedulerStrings
import com.mle.musicpimp.scheduler.{ClockPlayback, PlaybackJob, APManager}
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.json.{JsonStrings, JsonMessages}
import play.api.libs.json.Json._
import play.api.mvc.Accepting
import play.api.mvc.SimpleResult

object Alarms
  extends Secured
  with AlarmEditor
  with JsonHandler
  with SchedulerStrings
  with Log {

  val AcceptsPimp = Accepting("application/vnd.musicpimp.v17+json")

  val jobWriter = new Writes[PlaybackJob] {
    override def writes(o: PlaybackJob): JsValue = obj(
      JsonStrings.TRACK -> toJson(o.trackInfo)
    )
  }
  implicit val alarmWriter = new Writes[ClockPlayback] {
    override def writes(o: ClockPlayback): JsValue = obj(
      JsonStrings.ID -> toJson(o.id),
      "job" -> toJson(o.job)(jobWriter),
      "when" -> toJson(o.when),
      "enabled" -> toJson(o.enabled)
    )
  }

  def alarms = PimpAction(implicit request => {
    def content = APManager.status
    respond(
      html = views.html.alarms(content),
      json = Json.toJson(content)
    )
  })

  def handleJson = PimpParsedAction(parse.json)(jsonRequest => {
    val json = jsonRequest.body
    log info s"User: ${jsonRequest.user} from: ${jsonRequest.remoteAddress} said: $json"
    onRequest(jsonRequest.body)
  })

  def tracks = PimpAction(implicit request => {
    val tracks = Library.tracksRecursive
    Ok(Json.toJson(tracks))
  })

  def paths = PimpAction(implicit request => {
    val tracks = Library.songPathsRecursive
    implicit val pathFormat = PlaybackJob.pathFormat
    Ok(Json.toJson(tracks))
  })

  private def onRequest(json: JsValue): SimpleResult = {
    val jsResult = handle(json)
    simpleResult(json, jsResult)
  }

  private def simpleResult[T](json: JsValue, result: JsResult[T]): SimpleResult = {
    result.fold(
      errors => BadRequest(JsonMessages.failure(s"Invalid JSON: $json. Errors: $errors.")),
      valid => Ok)
  }
}