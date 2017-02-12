package controllers.pimpcloud

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.musicpimp.messaging.{PushTask, Pusher}
import com.malliina.pimpcloud.ErrorResponse
import controllers.pimpcloud.Push.{Body, Cmd, PushValue, ResultKey}
import play.api.libs.json.{JsError, JsResult, JsValue, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.Future

class Push(pusher: Pusher) extends Controller {
  def push = Action.async(parse.json) { request =>
    val payload = request.body
    parseJson(payload)
      .map(task => pusher.push(task).map(result => Ok(Json.obj(ResultKey -> result))))
      .getOrElse(Future.successful(BadRequest(ErrorResponse.simple(s"Invalid payload: $payload"))))
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
