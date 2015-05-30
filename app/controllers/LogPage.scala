package controllers

import ch.qos.logback.classic.Level
import com.mle.util.Logging
import play.api.data.{Forms, Form}
import play.api.mvc.RequestHeader
import views.html

/**
 * @author mle
 */
object LogPage extends HtmlController {
  val LEVEL = "level"

  val levelForm = Form[Level](LEVEL -> Forms.nonEmptyText.transform(Level.toLevel, (l: Level) => l.toString))

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
        Redirect(routes.LogPage.logs())
      }
    )
  })

  private def logPage(form: Form[Level])(implicit req: RequestHeader) = html.logs(form(LEVEL), Logging.levels, Logging.level)
}
