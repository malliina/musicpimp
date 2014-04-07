package com.mle.musicpimp.audio

import scala.concurrent.duration.Duration
import com.mle.storage.StorageSize
import java.io.InputStream
import play.api.libs.json.Json
import com.mle.play.json.JsonFormats2

/**
 *
 * @author mle
 */
case class BaseTrackMeta(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize) extends TrackMeta {
  def buildTrack(inStream: InputStream) = StreamedTrack(id, title, artist, album, duration, size, inStream)
}

object BaseTrackMeta {
  //  val empty = BaseTrackMeta("Unknown id", "Unknown title", "Unknown artist", "Unknown album", 0.seconds, 1.bytes)
  implicit val dur = JsonFormats2.durationFormat
  implicit val storage = JsonFormats2.storageSizeFormat
  implicit val jsonFormat = Json.format[BaseTrackMeta]
}