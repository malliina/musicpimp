package com.mle.musicpimp.audio

import java.io.InputStream

import com.mle.storage.StorageSize

import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
case class StreamedTrack(id: String,
                         title: String,
                         artist: String,
                         album: String,
                         duration: Duration,
                         size: StorageSize,
                         stream: InputStream) extends PlayableTrack {
  override def buildPlayer(eom: () => Unit): PimpPlayer = new StreamPlayer(this, eom)
}