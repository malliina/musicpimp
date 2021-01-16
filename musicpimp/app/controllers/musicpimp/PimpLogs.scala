package controllers.musicpimp

import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import com.malliina.logback.akka.DefaultAkkaAppender
import com.malliina.logback.LogbackUtils
import com.malliina.musicpimp.auth.Auths
import com.malliina.musicpimp.models.Errors
import com.malliina.play.ActorExecution
import controllers.musicpimp.PimpLogs.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.WebSocket

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object PimpLogs {
  private val log = Logger(getClass)
}

class PimpLogs(ctx: ActorExecution) {
  implicit val ec: ExecutionContext = ctx.executionContext
  implicit val mat: Materializer = ctx.materializer
  val appenderName = "AKKA"
  lazy val appender = LogbackUtils.getAppender[DefaultAkkaAppender](appenderName)
  lazy val jsonEvents =
    appender.logEvents.groupedWithin(5, 100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  def openSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    Auths.session.authenticate(rh).map { authResult =>
      authResult.map { ok =>
        log.info(s"Opening logs socket for '${ok.user}'...")
        Flow.fromSinkAndSource(Sink.ignore, jsonEvents)
      }.left.map { err =>
        log.error(s"Unauthorized request '$rh': '$err'.")
        Errors.accessDenied
      }
    }
  }
}
