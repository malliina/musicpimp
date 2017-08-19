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
                         storageSize: StorageSize,
                         stream: InputStream) extends PlayableTrack {
  override def buildPlayer(eom: () => Unit): PimpPlayer = new StreamPlayer(this, eom)
}

object StreamedTrack {
  def fromTrack(t: TrackMeta, inStream: InputStream): StreamedTrack =
    StreamedTrack(t.id, t.title, t.artist, t.album, t.path, t.duration, t.storageSize, inStream)
}
