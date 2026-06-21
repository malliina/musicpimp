package com.malliina.musicpimp.scheduler

import com.malliina.musicpimp.scheduler.PlaybackScheduler.log
import play.api.Logger

object PlaybackScheduler:
  private val log = Logger(getClass)

class PlaybackScheduler[S <: DaySchedule](s: IScheduler):
  private var scheduled = Map.empty[String, PlaybackJob]

  def saved = scheduled.values.toSeq

  def schedule(ap: PlaybackJob): String =
    val taskId = s.schedule(ap.when, ap)
    scheduled += (taskId -> ap)
    log.info(s"Scheduled task with task ID: $taskId. Description: ${ap.describe}")
    taskId

  def deschedule(id: String): Option[PlaybackJob] =
    val pairOpt = scheduled.find(pair => pair._2.id.contains(id))
    pairOpt.foreach:
      case (taskId, ap) =>
        s.cancel(taskId)
        scheduled -= taskId
        log.info(s"Descheduled task with ID: $id. Description: ${ap.describe}")
    pairOpt.map(_._2)

  def clear(): Unit =
    scheduled.keys.foreach(deschedule)
    scheduled = Map.empty[String, PlaybackJob]
