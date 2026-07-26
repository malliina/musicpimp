package com.malliina.musicpimp.scheduler

import cats.effect.Sync
import it.sauronsoftware.cron4j.SchedulingPattern

trait IScheduler[F[_]]:
  type TaskId = String

  /** Primitive.
    *
    * @param cron
    *   cron string
    * @param job
    *   code to run
    * @return
    *   the task id
    */
  def schedule(cron: String)(job: F[Unit]): TaskId

  def scheduleWithInterval(
    interval: Int,
    timeUnit: TimeUnit,
    days: Seq[WeekDay] = WeekDay.EveryDay
  )(f: F[Unit]): TaskId

  def scheduleAt(hour: Int, minute: Int, days: Seq[WeekDay] = WeekDay.EveryDay)(f: F[Unit]): TaskId

  def schedule(schedule: Schedule, job: Job[F]): TaskId

  def cancel(id: TaskId): Unit

  def start(): Unit

  def stop(): Unit

trait Schedule:
  def cronPattern: SchedulingPattern

trait DaySchedule extends Schedule:
  def days: Seq[WeekDay]

  def daysStringified = days.map(_.shortName).mkString(",")

  def daysReadable = days.map(_.longName).mkString(", ")

  def describe: String

trait Job[F[_]]:
  def describe: String

  def run(): F[Unit]

trait ActionPoint[F[_], J <: Job[F], S <: DaySchedule]:
  def id: Option[String]

  def enabled: Boolean

  def job: J

  def when: S

  def describe = job.describe + " " + when.describe

trait PlaybackAP[F[_]: Sync, S <: DaySchedule] extends ActionPoint[F, PlaybackJob[F], S]

trait AP[F[_]: Sync] extends PlaybackAP[F, DaySchedule]
