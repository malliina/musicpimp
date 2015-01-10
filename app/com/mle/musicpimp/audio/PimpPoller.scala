package com.mle.musicpimp.audio

import com.mle.util.Scheduling

import scala.concurrent.duration.{Duration, DurationInt}

/**
 *
 * @author Michael
 */
abstract class PimpPoller(pollInterval: Duration) extends AutoCloseable {
  val task = Scheduling.every(pollInterval.toSeconds.toInt.seconds) {
    OnUpdate()
  }

  def OnUpdate()

  def close(): Unit = task.cancel(true)
}