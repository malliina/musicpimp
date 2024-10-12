package controllers.pimpcloud

import com.malliina.http.PlayCirce
import com.malliina.musicpimp.messaging.Pusher
import com.malliina.musicpimp.messaging.cloud.{PushResponse, PushTask}
import com.malliina.musicpimp.models.Errors
import com.malliina.pimpcloud.json.JsonStrings.{Body, Cmd, PushValue}
import io.circe.syntax.EncoderOps
import io.circe.{Decoder, DecodingFailure, Json}
import play.api.mvc.{AbstractController, ControllerComponents}

import scala.concurrent.{ExecutionContext, Future}

class Push(comps: ControllerComponents, pusher: Pusher)
  extends AbstractController(comps)
  with PlayCirce:
  implicit val ec: ExecutionContext = defaultExecutionContext

  def push = Action.async(circeParser(comps.parsers.json)): request =>
    val payload = request.body
    parseJson(payload)
      .map(task => pusher.push(task).map(result => Ok(PushResponse(result).asJson)))
      .getOrElse(Future.successful(Errors.badRequest(s"Invalid payload: '$payload'.")))

  def parseJson(json: Json): Decoder.Result[PushTask] =
    json.hcursor
      .downField(Cmd)
      .as[String]
      .flatMap:
        case PushValue => json.hcursor.downField(Body).as[PushTask]
        case other     => Left(DecodingFailure(s"Unknown $Cmd: $other", Nil))

object Push:
  val ResultKey = "result"
