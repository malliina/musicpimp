package com.mle.musicpimp.audio

import com.mle.util.Scheduling
import com.mle.util.Implicits.int2timeUnits
import scala.concurrent.duration.Duration

/**
 *
 * @author Michael
 */
abstract class PimpPoller(pollInterval: Duration) extends AutoCloseable {
  val task = Scheduling.every(pollInterval.toSeconds.toInt seconds) {
    OnUpdate()
  }

  def OnUpdate()

  def close(): Unit = task.cancel(true)
}