package controllers.musicpimp

import ch.qos.logback.classic.Level
import com.malliina.logbackrx.LogEvent
import com.malliina.musicpimp.tags.PimpHtml
import com.malliina.util.Logging
import controllers.musicpimp.LogPage.log
import play.api.Logger
import play.api.data.{Form, Forms}
import play.api.libs.json.Json
import play.api.mvc.Result

case class FrontLogEvents(events: Seq[FrontLogEvent])

object FrontLogEvents {
  implicit val eventJson = FrontLogEvent.json
  implicit val json = Json.format[FrontLogEvents]
}

case class FrontLogEvent(message: String, module: String, level: Level)

object FrontLogEvent {
  implicit val levelJson = LogEvent.LevelFormat
  implicit val json = Json.format[FrontLogEvent]
}

class LogPage(tags: PimpHtml,
              sockets: PimpLogs,
              auth: AuthDeps)
  extends HtmlController(auth) {
  val LevelKey = "level"

  val levelForm = Form[Level](LevelKey -> Forms.nonEmptyText.transform(Level.toLevel, (l: Level) => l.toString))
  val frontLog = Logger("frontend")

  def logs = navigate { req =>
    logPage(levelForm, req)
  }

  def frontendLog = pimpParsedAction(parsers.json) { req =>
    implicit val json = FrontLogEvents.json

    req.body.validate[FrontLogEvents].fold[Result](
      invalid => {
        val msg = "Invalid JSON for log events."
        log.warn(s"$msg $invalid")
        badRequest(msg)
      },
      es => {
        es.events foreach handleLog
        Accepted
      }
    )
  }

  def handleLog(event: FrontLogEvent): Unit = {
    logFunc(Logger(event.module), event.level)(event.message)
  }

  def logFunc(logger: Logger, level: Level): String => Unit =
    if (level == Level.DEBUG) logger.debug(_)
    else if (level == Level.INFO) logger.info(_)
    else if (level == Level.WARN) logger.warn(_)
    else if (level == Level.ERROR) logger.error(_)
    else logger.trace(_)

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
    tags.logs(form(LevelKey), Logging.levels, Logging.level, req.user, None)
}

object LogPage {
  private val log = Logger(getClass)
}
