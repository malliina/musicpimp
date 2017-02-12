package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class Track(id: TrackID,
                 title: String,
                 album: String,
                 artist: String,
                 duration: Duration,
                 size: StorageSize) extends MusicItem

object Track {
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat
  implicit val format = Json.format[Track]
}
