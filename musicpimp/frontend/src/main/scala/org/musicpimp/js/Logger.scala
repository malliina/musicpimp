package org.musicpimp.js

import com.malliina.musicpimp.models.{FrontLogEvent, FrontLogEvents}

class Logger(moduleName: String) extends BaseLogger {
  def info(message: String): Unit = {
    val events = FrontLogEvents.single(message, moduleName)
    BaseScript.postAjax("/logs", events)
  }

  def error(t: Throwable): Unit = {
    val message = FrontLogEvent.error(s"An exception occurred: ${t.getMessage}", "generic")
    val events = FrontLogEvents(Seq(message))
    BaseScript.postAjax("/logs", events)
  }
}

object Logger {
  val FrontendModule = "frontend"
  val default = Logger(FrontendModule)

  def apply(moduleName: String) = new Logger(moduleName)
}
