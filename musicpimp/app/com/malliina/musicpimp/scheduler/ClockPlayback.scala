package com.malliina.musicpimp.scheduler

import play.api.libs.json.Json

case class ClockPlayback(id: Option[String],
                         job: PlaybackJob,
                         when: ClockSchedule,
                         enabled: Boolean)
  extends PlaybackAP[ClockSchedule]

object ClockPlayback {
  val shortJson = Json.format[ClockPlayback]
}
