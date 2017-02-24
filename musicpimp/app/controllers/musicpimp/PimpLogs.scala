package controllers.musicpimp

import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.play.{ActorExecution, PimpSockets}
import play.api.libs.json.{JsValue, Json}
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

class PimpLogs(ctx: ActorExecution) {
  val appenderName = "RX"

  lazy val appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)
  lazy val jsonEvents: Observable[JsValue] =
    appender.logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  val sockets = PimpSockets.observingSockets(jsonEvents, CloudWS.sessionAuth, ctx)

  def openSocket = sockets.newSocket
}
