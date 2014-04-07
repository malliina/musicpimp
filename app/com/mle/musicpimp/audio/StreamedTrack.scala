package com.mle.musicpimp.audio

import scala.concurrent.duration.Duration
import com.mle.storage.StorageSize
import java.io.InputStream

/**
 *
 * @author mle
 */
case class StreamedTrack(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize, stream: InputStream) extends PlayableTrack {
  override def buildPlayer(): PimpPlayer = new StreamPlayer(this)
}