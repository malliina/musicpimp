package com.mle.musicpimp.audio

import scala.concurrent.duration.Duration
import com.mle.storage.StorageSize

/**
 *
 * @author mle
 */
trait ITrackMeta {
  def id: String

  def title: String

  def artist: String

  def album: String

  def duration: Duration

  def size: StorageSize
}
