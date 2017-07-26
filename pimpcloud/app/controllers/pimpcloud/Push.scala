package controllers.pimpcloud

import com.malliina.musicpimp.messaging.Pusher
import com.malliina.musicpimp.messaging.cloud.PushTask
import com.malliina.musicpimp.models.Errors
import controllers.pimpcloud.Push.{Body, Cmd, PushValue, ResultKey}
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.Future

class Push(comps: ControllerComponents, pusher: Pusher) extends AbstractController(comps) {
  implicit val ec = defaultExecutionContext

  def push = Action.async(parse.json) { request =>
    val payload = request.body
    parseJson(payload)
      .map(task => pusher.push(task).map(result => Ok(Json.obj(ResultKey -> result))))
      .getOrElse(Future.successful(Errors.badRequest(s"Invalid payload: '$payload'.")))
  }

  def parseJson(json: JsValue): JsResult[PushTask] = {
    (json \ Cmd).validate[String].flatMap {
      case PushValue => (json \ Body).validate[PushTask]
      case other => JsError(s"Unknown $Cmd: $other")
    }
  }
}

object Push {
  val Body = "body"
  val Cmd = "cmd"
  val PushValue = "push"
  val ResultKey = "result"
}
