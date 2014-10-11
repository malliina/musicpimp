package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import com.mle.play.ws.SyncAuth
import play.api.mvc.Call
import views.html

/**
 *
 * @author mle
 */
object PimpLogs extends LogStreaming with SyncAuth with HtmlController {
  val appenderName = "RX"

  override def appenderOpt = LogbackUtils.appender[BasicBoundedReplayRxAppender](appenderName)

  def feedback =
    if (appenderOpt.isEmpty) {
      val clazzName = classOf[BasicBoundedReplayRxAppender].getName
      Some(s"Unable to find log source. If you have modified the Logback configuration, please add an appender of class $clazzName named $appenderName to your Logback configuration.")
    } else {
      None
    }

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  def logs = navigate(implicit req => html.logs(feedback))
}