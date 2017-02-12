package org.musicpimp.js

case class FrontLogEvent(message: String, module: String, level: String)

object FrontLogEvent {
  val Info = "info"

  def simple(message: String, module: String) =
    FrontLogEvent(message, module, Info)
}

case class FrontLogEvents(events: Seq[FrontLogEvent])

object FrontLogEvents {
  def single(message: String, module: String) =
    FrontLogEvents(Seq(FrontLogEvent.simple(message, module)))
}

class Logger(moduleName: String) {
  def info(message: String) =
    BaseScript.postAjax("/logs", FrontLogEvents.single(message, moduleName))
}

object Logger {
  val FrontendModule = "frontend"
  val default = Logger(FrontendModule)

  def apply(moduleName: String) = new Logger(moduleName)
}
