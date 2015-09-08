package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.{AuthResult, BaseSecurity, LogStreaming}
import com.mle.play.ws.SyncAuth
import com.mle.util.Log
import play.api.libs.json.{Json, JsValue}
import play.api.mvc.{Security, RequestHeader, Call}
import rx.lang.scala.Observable
import concurrent.duration.DurationInt

/**
 *
 * @author mle
 */
class PimpLogs extends LogStreaming with SyncAuth with Log {
  val appenderName = "RX"

  override lazy val jsonEvents: Observable[JsValue] = logEvents.tumblingBuffer(100.millis).filter(_.nonEmpty).map(Json.toJson(_))

  override def appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  override def authenticate(implicit req: RequestHeader): Option[AuthResult] =
    req.session.get(Security.username).map(AuthResult.apply(_, None))
}
