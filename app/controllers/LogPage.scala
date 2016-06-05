package controllers

import akka.stream.Materializer
import ch.qos.logback.classic.Level
import com.malliina.play.Authenticator
import com.malliina.util.Logging
import controllers.LogPage.log
import play.api.Logger
import play.api.data.{Form, Forms}
import play.api.mvc.RequestHeader
import views.html

class LogPage(sockets: PimpLogs, auth: Authenticator, mat: Materializer)
  extends HtmlController(auth, mat) {
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

  private def logPage(form: Form[Level])(implicit req: RequestHeader) =
    html.logs(sockets.wsUrl(req), form(LEVEL), Logging.levels, Logging.level)
}

object LogPage {
  private val log = Logger(getClass)
}
