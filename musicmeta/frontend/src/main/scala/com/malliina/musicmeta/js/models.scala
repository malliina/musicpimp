package com.malliina.musicmeta.js

import play.api.libs.json.{Json, OFormat}

case class LogEvent(timestamp: Long,
                    timeFormatted: String,
                    message: String,
                    loggerName: String,
                    threadName: String,
                    level: String) {
  def isError = level == "ERROR"
}

object LogEvent {
  implicit val json: OFormat[LogEvent] = Json.format[LogEvent]
}

case class LogEvents(events: Seq[LogEvent])

object LogEvents {
  implicit val json: OFormat[LogEvents] = Json.format[LogEvents]
}
