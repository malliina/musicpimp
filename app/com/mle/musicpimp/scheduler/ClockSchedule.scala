package com.mle.musicpimp.scheduler

import it.sauronsoftware.cron4j.SchedulingPattern
import play.api.libs.json.Json

/**
 * Schedule for regularly executing something at a given time of day.
 *
 * @param hour [0, 23] and *
 * @param minute [0, 59] and *
 * @param days the weekdays during which this schedule is valid
 * @return the cron pattern
 */
case class ClockSchedule(hour: Int, minute: Int, days: Seq[WeekDay]) extends DaySchedule {
  def cronPattern = new SchedulingPattern(s"$minute $hour * * $daysStringified")

  private val daysDescribed = if (days.toSet == WeekDay.EveryDaySet) "every day" else s"on $daysReadable"

  def timeFormatted = maybePrependZero(hour) + ":" + maybePrependZero(minute)

  def describe: String = s"at $timeFormatted $daysDescribed"

  private def maybePrependZero(i: Int) = if (i < 10) s"0$i" else s"$i"
}

object ClockSchedule {
  implicit val format = Json.format[ClockSchedule]
}