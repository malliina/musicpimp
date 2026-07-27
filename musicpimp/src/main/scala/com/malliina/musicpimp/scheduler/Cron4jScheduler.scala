package com.malliina.musicpimp.scheduler

import cats.effect.std.Dispatcher
import it.sauronsoftware.cron4j.{Scheduler, SchedulingPattern}

class Cron4jScheduler[F[_]](d: Dispatcher[F]) extends IScheduler[F]:
  val s = new Scheduler

  override def start(): Unit = s.start()

  override def stop(): Unit = s.stop()

  override def schedule(cron: String)(job: F[Unit]): TaskId =
    val runnable = new Runnable:
      override def run(): Unit = d.unsafeRunAndForget(job)
    s.schedule(cron, runnable)

  def schedule(cronPattern: SchedulingPattern)(job: F[Unit]): TaskId =
    schedule(cronPattern.toString)(job)

  override def schedule(when: Schedule, job: Job[F]): TaskId =
    schedule(when.cronPattern)(job.run())

  def schedule(when: Schedule)(job: F[Unit]): TaskId =
    schedule(when.cronPattern)(job)

  override def scheduleWithInterval(
    interval: Int,
    timeUnit: TimeUnit,
    days: Seq[WeekDay] = WeekDay.EveryDay
  )(f: F[Unit]): TaskId =
    schedule(IntervalSchedule(interval, timeUnit, days))(f)

  override def scheduleAt(hour: Int, minute: Int, days: Seq[WeekDay] = WeekDay.EveryDay)(
    f: F[Unit]
  ): TaskId =
    schedule(ClockSchedule(hour, minute, days))(f)

  override def cancel(id: TaskId): Unit =
    s.deschedule(id)
