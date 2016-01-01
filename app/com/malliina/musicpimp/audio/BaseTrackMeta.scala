package com.malliina.musicpimp.audio

import java.io.InputStream

import com.malliina.play.json.JsonFormats
import com.malliina.storage.StorageSize
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

/**
  *
  * @author mle
  */
case class BaseTrackMeta(id: String,
                         title: String,
                         artist: String,
                         album: String,
                         duration: Duration,
                         size: StorageSize) extends TrackMeta {
  def buildTrack(inStream: InputStream) = StreamedTrack(id, title, artist, album, duration, size, inStream)
}

object BaseTrackMeta {
  implicit val dur = JsonFormats.durationFormat
  implicit val storage = JsonFormats.storageSizeFormat
  implicit val jsonFormat = Json.format[BaseTrackMeta]
}
