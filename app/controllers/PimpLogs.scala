package controllers

import ch.qos.logback.classic.Level
import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import com.mle.play.controllers.LogStreaming
import com.mle.play.ws.SyncAuth
import com.mle.util.{Log, Logging}
import play.api.data.{Form, Forms}
import play.api.mvc.{Call, RequestHeader}
import views.html

/**
 *
 * @author mle
 */
object PimpLogs extends LogStreaming with SyncAuth with HtmlController with Log {
  val appenderName = "RX"
  val LEVEL = "level"

  val levelForm = Form[Level](LEVEL -> Forms.nonEmptyText.transform(Level.toLevel, (l: Level) => l.toString))

  override def appender = LogbackUtils.getAppender[BasicBoundedReplayRxAppender](appenderName)

  def logs = navigate(implicit req => logPage(levelForm))

  def changeLogLevel = PimpAction(implicit req => {
    levelForm.bindFromRequest.fold(
      erroredForm => {
        log warn s"Log level change submission failed"
        BadRequest(logPage(erroredForm))
      },
      level => {
        Logging.level = level
        log warn s"Changed log level to $level"
        Redirect(routes.PimpLogs.logs())
      }
    )
  })

  override def openSocketCall: Call = routes.PimpLogs.openSocket

  private def logPage(form: Form[Level])(implicit req: RequestHeader) = html.logs(form(LEVEL), Logging.levels, Logging.level)

  //  def feedback =
  //    if (appenderOpt.isEmpty) {
  //      val clazzName = classOf[BasicBoundedReplayRxAppender].getName
  //      Some(s"Unable to find log source. If you have modified the Logback configuration, please add an appender of class $clazzName named $appenderName to your Logback configuration.")
  //    } else {
  //      None
  //    }
}
