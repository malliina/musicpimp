package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.{AuthResult, BaseSecurity, LogStreaming}
import com.mle.play.ws.SyncAuth
import com.mle.util.Log
import play.api.mvc.{Security, RequestHeader, Call}

/**
 *
 * @author mle
 */
object PimpLogs extends LogStreaming with SyncAuth with Log {
  val appenderName = "RX"

  override def appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  override def authenticate(implicit req: RequestHeader): Option[AuthResult] =
    req.session.get(Security.username).map(AuthResult.apply(_, None))
}
