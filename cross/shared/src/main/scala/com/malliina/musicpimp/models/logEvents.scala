package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class FrontLogEvent(message: String, module: String, level: String)

object FrontLogEvent {
  implicit val json = Json.format[FrontLogEvent]

  val Info = "info"
  val Error = "error"

  def simple(message: String, module: String): FrontLogEvent =
    FrontLogEvent(message, module, Info)

  def error(message: String, module: String): FrontLogEvent =
    FrontLogEvent(message, module, Error)
}

case class FrontLogEvents(events: Seq[FrontLogEvent])

object FrontLogEvents {
  implicit val json = Json.format[FrontLogEvents]

  def single(message: String, module: String) =
    FrontLogEvents(Seq(FrontLogEvent.simple(message, module)))
}

case class JVMLogEntry(level: String,
                       message: String,
                       loggerName: String,
                       threadName: String,
                       timeFormatted: String,
                       stackTrace: Option[String] = None)

object JVMLogEntry {
  implicit val json = Json.format[JVMLogEntry]
}
