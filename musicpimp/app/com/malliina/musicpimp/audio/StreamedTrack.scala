package com.malliina.musicpimp.audio

import java.io.InputStream

import akka.stream.Materializer
import com.malliina.musicpimp.models.TrackID
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath

import scala.concurrent.duration.Duration

case class StreamedTrack(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  path: UnixPath,
  duration: Duration,
  size: StorageSize,
  stream: InputStream
)(implicit mat: Materializer)
  extends PlayableTrack {
  override def buildPlayer(eom: () => Unit)(implicit mat: Materializer): PimpPlayer =
    new StreamPlayer(this, eom)
}

object StreamedTrack {
  def fromTrack(t: TrackMeta, inStream: InputStream, mat: Materializer): StreamedTrack =
    StreamedTrack(t.id, t.title, t.artist, t.album, t.path, t.duration, t.size, inStream)(mat)
}
