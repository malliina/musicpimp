package controllers.pimpcloud

import akka.stream.scaladsl.{Flow, Sink}
import com.malliina.logback.akka.DefaultAkkaAppender
import com.malliina.musicpimp.models.{Errors, JVMLogEntry}
import com.malliina.play.ActorExecution
import com.malliina.play.tags.TagPage
import controllers.pimpcloud.Logs.log
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{DefaultActionBuilder, WebSocket}
import com.malliina.logback.LogbackUtils
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Logs {
  private val log = Logger(getClass)
}

class Logs(tags: CloudTags, auth: PimpAuth, ctx: ActorExecution, actions: DefaultActionBuilder) {
  implicit val ec: ExecutionContext = ctx.executionContext

  val appenderName = "AKKA"
  lazy val jsonEvents = LogbackUtils
    .getAppender[DefaultAkkaAppender](appenderName)
    .logEvents
    .map { e =>
      JVMLogEntry(
        e.level.levelStr,
        e.message,
        e.loggerName,
        e.threadName,
        e.timeFormatted,
        e.stackTrace
      )
    }
    .groupedWithin(5, 100.millis)
    .filter(_.nonEmpty)
    .map(Json.toJson(_))
  val logoutMessageKey = "message"

  def index = auth.navigate[TagPage](_ => tags.admin)

  def logs = auth.navigate[TagPage](_ => tags.logs)

  // HTML
  def logout = auth.authAction { _ =>
    Redirect(routes.Logs.eject)
      .flashing(logoutMessageKey -> "You have now logged out.")
      .withNewSession
  }

  def eject = auth.logged(actions(req => Ok(tags.eject(req.flash.get(logoutMessageKey)))))

  def openSocket = WebSocket.acceptOrResult[JsValue, JsValue] { rh =>
    auth.authenticate(rh).map { authResult =>
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
