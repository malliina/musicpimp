package controllers

import views.html
import com.mle.musicpimp.log.PimpLog

/**
 *
 * @author mle
 */
trait Logs extends HtmlController {
  def logs = navigate(html.logs(loadLogEntries))

  def loadLogEntries = PimpLog.usingLog(_.toSeq.reverse.take(1000))
}
