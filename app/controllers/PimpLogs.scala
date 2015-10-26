package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.maps.{ItemMap, StmItemMap}
import com.mle.play.controllers.{AuthResult, LogStreaming}
import com.mle.play.ws.{SyncAuth, WebSocketClient}
import com.mle.util.Log
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Call, RequestHeader, Security}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationInt

/**
 *
 * @author mle
 */
class PimpLogs extends LogStreaming with SyncAuth with Log {
  val appenderName = "RX"

  override lazy val subscriptions: ItemMap[WebSocketClient, Subscription] = StmItemMap.empty[WebSocketClient, Subscription]

  override lazy val jsonEvents: Observable[JsValue] = logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  override def appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  override def authenticate(implicit req: RequestHeader): Option[AuthResult] =
    req.session.get(Security.username).map(AuthResult.apply(_, None))
}
