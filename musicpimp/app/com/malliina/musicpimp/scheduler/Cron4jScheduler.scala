package com.malliina.musicpimp.scheduler

import it.sauronsoftware.cron4j.{Scheduler, SchedulingPattern}

trait Cron4jScheduler extends IScheduler:
  val s = new Scheduler

  override def start(): Unit = s.start()

  override def stop(): Unit = s.stop()

  override def schedule(cron: String)(job: => Any): TaskId =
    val runnable = new Runnable:
      override def run(): Unit = job
    s.schedule(cron, runnable)

  def schedule(cronPattern: SchedulingPattern)(job: => Any): TaskId =
    schedule(cronPattern.toString)(job)

  override def schedule(when: Schedule, job: Job): TaskId =
    schedule(when.cronPattern)(job.run())

  def schedule(when: Schedule)(job: => Any): TaskId =
    schedule(when.cronPattern)(job)

  override def scheduleWithInterval(
    interval: Int,
    timeUnit: TimeUnit,
    days: Seq[WeekDay] = WeekDay.EveryDay
  )(f: => Any): TaskId =
    schedule(IntervalSchedule(interval, timeUnit, days))(f)

  override def scheduleAt(hour: Int, minute: Int, days: Seq[WeekDay] = WeekDay.EveryDay)(
    f: => Any
  ): TaskId =
    schedule(ClockSchedule(hour, minute, days))(f)

  override def cancel(id: TaskId): Unit =
    s.deschedule(id)

object Cron4jScheduler extends Cron4jScheduler
