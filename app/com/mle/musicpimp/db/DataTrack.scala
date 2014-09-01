package com.mle.musicpimp.db

import com.mle.play.json.JsonFormats
import com.mle.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json

import scala.concurrent.duration.{Duration, DurationInt}

/**
 * @author Michael
 */
case class DataTrack(id: String, title: String, artist: String, album: String, duration: Duration, size: StorageSize) {
  def toValues = Some((id, title, artist, album, duration.toSeconds.toInt, size.toBytes))
}

object DataTrack {
  def fromValues(i: String, ti: String, ar: String, al: String, du: Int, si: Long) =
    DataTrack(i, ti, ar, al, du.seconds, si.bytes)

  implicit val durJson = JsonFormats.durationFormat
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val format = Json.format[DataTrack]
}