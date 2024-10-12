package controllers.pimpcloud

import com.malliina.logback.{LogbackUtils, PimpAppender}
import com.malliina.musicpimp.models.{Errors, JVMLogEntry}
import com.malliina.play.ActorExecution
import com.malliina.play.tags.TagPage
import controllers.pimpcloud.Logs.log
import io.circe.Json
import io.circe.syntax.EncoderOps
import org.apache.pekko.stream.scaladsl.{Flow, Sink}
import play.api.Logger
import play.api.mvc.Results.{Ok, Redirect}
import play.api.mvc.{DefaultActionBuilder, WebSocket}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

object Logs:
  private val log = Logger(getClass)

class Logs(tags: CloudTags, auth: PimpAuth, ctx: ActorExecution, actions: DefaultActionBuilder):
  implicit val ec: ExecutionContext = ctx.executionContext

  val appenderName = "AKKA"
  lazy val jsonEvents = LogbackUtils
    .getAppender[PimpAppender](appenderName)
    .logEvents
    .map: e =>
      JVMLogEntry(
        e.level.levelStr,
        e.message,
        e.loggerName,
        e.threadName,
        e.timeFormatted,
        e.stackTrace
      )
    .groupWithin(5, 100.millis)
    .filter(_.nonEmpty)
    .map(e => e.toList.asJson)
  val logoutMessageKey = "message"

  def index = auth.navigate[TagPage](_ => tags.admin)

  def logs = auth.navigate[TagPage](_ => tags.logs)

  // HTML
  def logout = auth.authAction: _ =>
    Redirect(routes.Logs.eject)
      .flashing(logoutMessageKey -> "You have now logged out.")
      .withNewSession

  def eject = auth.logged(actions(req => Ok(tags.eject(req.flash.get(logoutMessageKey)))))

  import PimpAppender.circeTransformer

  def openSocket = WebSocket.acceptOrResult[Json, Json]: rh =>
    auth
      .authenticate(rh)
      .map: authResult =>
        authResult
          .map: ok =>
            log.info(s"Opening logs socket for '${ok.user}'...")
            Flow.fromSinkAndSource(Sink.ignore, PimpAppender.fs2StreamToAkkaSource(jsonEvents))
          .left
          .map: err =>
            log.error(s"Unauthorized request '$rh': '$err'.")
            Errors.accessDenied
