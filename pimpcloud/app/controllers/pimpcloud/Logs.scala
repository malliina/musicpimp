package controllers.pimpcloud

import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogEvent, LogbackUtils}
import com.malliina.play.{ActorExecution, PimpSockets}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

class Logs(tags: CloudTags, auth: PimpAuth, ctx: ActorExecution) {
  val appenderName = "RX"
  lazy val appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)
  lazy val logEvents: Observable[LogEvent] = appender.logEvents
  lazy val jsonEvents: Observable[JsValue] =
    logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))
  lazy val sockets = PimpSockets.observingSockets(jsonEvents, auth, ctx)

  def index = auth.navigate(_ => tags.admin)

  def logs = auth.navigate(_ => tags.logs)

  def openSocket = sockets.newSocket
}

object Logs {
  private val log = Logger(getClass)
}
