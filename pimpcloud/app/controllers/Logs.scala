package controllers

import akka.stream.Materializer
import com.malliina.logbackrx.RxLogback.EventMapping
import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogEvent, LogbackUtils}
import play.api.Logger
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

class Logs(tags: CloudTags, auth: PimpAuth, mat: Materializer) extends AdminStreaming(auth, mat) {
  def logEvents: Observable[LogEvent] = appender.logEvents

  override lazy val jsonEvents: Observable[JsValue] =
    logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  def appender: EventMapping =
    LogbackUtils.getAppender[BasicBoundedReplayRxAppender]("RX")

  override def openSocketCall: Call = routes.Logs.openSocket()

  def index = auth.navigate(_ => tags.admin)

  def logs = auth.navigate(_ => tags.logs)
}

object Logs {
  private val log = Logger(getClass)
}
