package com.mle.musicpimp.scheduler

import com.mle.util.Log

/**
 *
 * @author mle
 */
class PlaybackScheduler[S <: DaySchedule, A <: PlaybackAP[S]](s: IScheduler) extends Log {
  private var scheduled = Map.empty[String, A]

  def saved = scheduled.values.toSeq

  def schedule(ap: A): String = {
    val taskId = s.schedule(ap.when, ap.job)
    scheduled += (taskId -> ap)
    log.info(s"Scheduled task with task ID: $taskId. Description: ${ap.describe}")
    taskId
  }

  def deschedule(id: String): Option[A] = {
    val pairOpt = scheduled.find(pair => pair._2.id == Some(id))
    pairOpt.foreach {
      case (taskId, ap) =>
        s.cancel(taskId)
        scheduled -= taskId
        log.info(s"Descheduled task with ID: $id. Description: ${ap.describe}")
    }
    pairOpt.map(_._2)
  }

  def clear(): Unit = {
    scheduled.keys.foreach(deschedule)
    scheduled = Map.empty[String, A]
  }
}

