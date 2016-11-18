package controllers

import akka.stream.Materializer
import com.malliina.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.malliina.maps.{ItemMap, StmItemMap}
import com.malliina.play.controllers.LogStreaming
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.Username
import com.malliina.play.ws.JsonSocketClient
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, RequestHeader, Security}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

class PimpLogs(mat: Materializer) extends LogStreaming(mat) {
  val appenderName = "RX"

  override lazy val subscriptions: ItemMap[JsonSocketClient[Username], Subscription] =
    StmItemMap.empty[JsonSocketClient[Username], Subscription]

  override lazy val jsonEvents: Observable[JsValue] =
    logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  override def appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  override def authenticateAsync(req: RequestHeader): Future[AuthedRequest] =
    toFuture(authenticate(req))

  def authenticate(request: RequestHeader): Option[AuthedRequest] =
    request.session.get(Security.username).map(name => new AuthedRequest(Username(name), request, None))

  def toFuture[T](opt: Option[T]) =
    opt.fold[Future[T]](Future failed new NoSuchElementException)(fut)
}
