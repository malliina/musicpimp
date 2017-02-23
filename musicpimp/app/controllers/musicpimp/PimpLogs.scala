package controllers.musicpimp

import akka.actor.{ActorRef, Props}
import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.play.ActorExecution
import com.malliina.play.http.AuthedRequest
import com.malliina.play.ws._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.RequestHeader
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationInt

case class ActorInfo(out: ActorRef, rh: RequestHeader) extends ActorMeta

class PimpLogs(ctx: ActorExecution) {
  val appenderName = "RX"

  lazy val appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)
  lazy val jsonEvents: Observable[JsValue] =
    appender.logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  val sockets = new Sockets(CloudWS.auth, ctx) {
    override def props(out: ActorRef, user: AuthedRequest, rh: RequestHeader) =
      Props(new ObserverActor(jsonEvents, ActorInfo(out, rh)))
  }

  def openSocket = sockets.newSocket
}
