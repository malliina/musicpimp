package com.malliina.musicpimp.db

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.library.Library
import com.malliina.play.json.JsonFormats
import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json

import scala.concurrent.duration.{Duration, DurationInt}

/**
 * @author Michael
 */
case class DataTrack(id: String,
                     title: String,
                     artist: String,
                     album: String,
                     duration: Duration,
                     size: StorageSize,
                     folder: String) extends TrackMeta {
  val path = Library decode folder

  def toValues = Some((id, title, artist, album, duration.toSeconds.toInt, size.toBytes, folder))
}

object DataTrack {
  def fromValues(i: String, ti: String, ar: String, al: String, du: Int, si: Long, fo: String) =
    DataTrack(i, ti, ar, al, du.seconds, si.bytes, fo)

  implicit val durJson = JsonFormats.durationFormat
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val format = Json.format[DataTrack]
}
