package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models.{PimpPath, TrackID}
import com.malliina.storage.StorageSize
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class Track(id: TrackID,
                 title: String,
                 artist: String,
                 album: String,
                 duration: Duration,
                 size: StorageSize) extends TrackMeta {
  override def path: PimpPath = PimpPath(PimpEnc relativePath id)
}

object Track {
  implicit val dur = JsonFormats.durationFormat
  implicit val storage = JsonFormats.storageSizeFormat
  implicit val jsonFormat = Json.format[Track]
}
