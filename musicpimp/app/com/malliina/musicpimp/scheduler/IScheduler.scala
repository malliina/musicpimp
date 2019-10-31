package com.malliina.musicpimp.scheduler

import it.sauronsoftware.cron4j.SchedulingPattern

trait IScheduler {
  type TaskId = String

  /** Primitive.
    *
    * @param cron cron string
    * @param job  code to run
    * @return the task id
    */
  def schedule(cron: String)(job: => Any): TaskId

  def scheduleWithInterval(
    interval: Int,
    timeUnit: TimeUnit,
    days: Seq[WeekDay] = WeekDay.EveryDay
  )(f: => Any): TaskId

  def scheduleAt(hour: Int, minute: Int, days: Seq[WeekDay] = WeekDay.EveryDay)(f: => Any): TaskId

  def schedule(schedule: Schedule, job: Job): TaskId

  def cancel(id: TaskId): Unit

  def start(): Unit

  def stop(): Unit
}

trait Schedule {
  def cronPattern: SchedulingPattern
}

trait DaySchedule extends Schedule {
  def days: Seq[WeekDay]

  def daysStringified = days.map(_.shortName).mkString(",")

  def daysReadable = days.map(_.longName).mkString(", ")

  def describe: String
}

trait Job {
  def describe: String

  def run(): Unit
}

trait ActionPoint[J <: Job, S <: DaySchedule] {
  def id: Option[String]

  def enabled: Boolean

  def job: J

  def when: S

  def describe = job.describe + " " + when.describe
}

trait PlaybackAP[S <: DaySchedule] extends ActionPoint[PlaybackJob, S]

trait AP extends PlaybackAP[DaySchedule]
