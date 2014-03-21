package com.mle.musicpimp.scheduler

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class ClockPlayback(id: Option[String], job: PlaybackJob, when: ClockSchedule, enabled: Boolean)
  extends PlaybackAP[ClockSchedule]

object ClockPlayback {
  implicit val json = Json.format[ClockPlayback]
}