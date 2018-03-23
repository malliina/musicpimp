package controllers.pimpcloud

import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.musicpimp.models.JVMLogEntry
import com.malliina.play.tags.TagPage
import com.malliina.play.{ActorExecution, PimpSockets}
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.Results.{Ok, Redirect}

import scala.concurrent.duration.DurationInt

class Logs(tags: CloudTags, auth: PimpAuth, ctx: ActorExecution, actions: DefaultActionBuilder) {
  val appenderName = "RX"
  lazy val jsonEvents = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)
    .logEvents
    .map(e => JVMLogEntry(e.level.levelStr, e.message, e.loggerName, e.threadName, e.timeFormatted, e.stackTrace))
    .tumblingBuffer(100.millis)
    .filter(_.nonEmpty)
    .map(Json.toJson(_))
  lazy val sockets = PimpSockets.observingSockets(jsonEvents, auth, ctx)
  val logoutMessageKey = "message"

  def index = auth.navigate[TagPage](_ => tags.admin)

  def logs = auth.navigate[TagPage](_ => tags.logs)

  // HTML
  def logout = auth.authAction(_ => Redirect(routes.Logs.eject()).flashing(logoutMessageKey -> "You have now logged out.").withNewSession)

  def eject = auth.logged(actions(req => Ok(tags.eject(req.flash.get(logoutMessageKey)))))

  def openSocket = sockets.newSocket
}

object Logs {
  private val log = Logger(getClass)
}
