package controllers

import com.mle.logbackrx.{BasicBoundedReplayRxAppender, LogbackUtils}
import controllers.PimpLogController._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Call
import rx.lang.scala.Observable
import views.html

/**
 *
 * @author mle
 */
trait PimpLogController extends StreamingLogController with HtmlController {
  override val logEvents: Observable[JsValue] = appenderOpt
    .map(_.logEvents.map(e => Json.toJson(e)))
    .getOrElse(Observable.empty)

  def feedback =
    if (appenderOpt.isEmpty) {
      val clazzName = classOf[BasicBoundedReplayRxAppender].getName
      Some(s"Unable to find log source. If you have modified the Logback configuration, please add an appender of class $clazzName named $appenderName to your Logback configuration.")
    } else {
      None
    }

  override def subscribeCall: Call = routes.Website.openLogSocket

  def logs = navigate(implicit req => html.logs(feedback))
}

object PimpLogController {
  val appenderName = "RX"

  def appenderOpt = LogbackUtils.appender[BasicBoundedReplayRxAppender](appenderName)
}
