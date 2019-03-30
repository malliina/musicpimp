package com.malliina.musicmeta.js

import play.api.libs.json.Json

case class LogEvent(timestamp: Long,
                    timeFormatted: String,
                    message: String,
                    loggerName: String,
                    threadName: String,
                    level: String) {
  def isError = level == "ERROR"
}

object LogEvent {
  implicit val json = Json.format[LogEvent]
}

case class LogEvents(events: Seq[LogEvent])

object LogEvents {
  implicit val json = Json.format[LogEvents]
}
