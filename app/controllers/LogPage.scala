package controllers

import akka.stream.Materializer
import ch.qos.logback.classic.Level
import com.malliina.musicpimp.tags.PimpTags
import com.malliina.play.Authenticator
import com.malliina.util.Logging
import controllers.LogPage.log
import play.api.Logger
import play.api.data.{Form, Forms}

class LogPage(tags: PimpTags, sockets: PimpLogs, auth: Authenticator, mat: Materializer)
  extends HtmlController(auth, mat) {
  val LEVEL = "level"

  val levelForm = Form[Level](LEVEL -> Forms.nonEmptyText.transform(Level.toLevel, (l: Level) => l.toString))

  def logs = navigate { req =>
    logPage(levelForm, req)
  }

  def changeLogLevel = pimpAction { req =>
    levelForm.bindFromRequest()(req).fold(
      erroredForm => {
        log warn s"Log level change submission failed"
        BadRequest(logPage(erroredForm, req))
      },
      level => {
        Logging.level = level
        log warn s"Changed log level to $level"
        Redirect(routes.LogPage.logs())
      }
    )
  }

  private def logPage(form: Form[Level], req: PimpRequest) =
    tags.logs(form(LEVEL), Logging.levels, Logging.level, req.user, None)
}

object LogPage {
  private val log = Logger(getClass)
}
