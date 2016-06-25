package com.malliina.musicpimp.audio

import java.io.InputStream

import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.PimpPath
import com.malliina.play.json.JsonFormats
import com.malliina.storage.StorageSize
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class BaseTrackMeta(id: String,
                         title: String,
                         artist: String,
                         album: String,
                         duration: Duration,
                         size: StorageSize) extends TrackMeta {

  override def path: PimpPath = PimpPath(Library relativePath id)

  def buildTrack(inStream: InputStream) =
    StreamedTrack(id, title, artist, album, path, duration, size, inStream)
}

object BaseTrackMeta {
  implicit val dur = JsonFormats.durationFormat
  implicit val storage = JsonFormats.storageSizeFormat
  implicit val jsonFormat = Json.format[BaseTrackMeta]
}
