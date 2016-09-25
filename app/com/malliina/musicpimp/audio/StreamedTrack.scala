package com.malliina.musicpimp.audio

import java.io.InputStream

import com.malliina.musicpimp.models.{PimpPath, TrackID}
import com.malliina.storage.StorageSize

import scala.concurrent.duration.Duration

case class StreamedTrack(id: TrackID,
                         title: String,
                         artist: String,
                         album: String,
                         path: PimpPath,
                         duration: Duration,
                         size: StorageSize,
                         stream: InputStream) extends PlayableTrack {
  override def buildPlayer(eom: () => Unit): PimpPlayer = new StreamPlayer(this, eom)
}
