package com.malliina.musicpimp.db

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.PimpPath
import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json
import slick.jdbc.GetResult

import scala.concurrent.duration.{Duration, DurationInt}

case class DataTrack(id: String,
                     title: String,
                     artist: String,
                     album: String,
                     duration: Duration,
                     size: StorageSize,
                     folder: String) extends TrackMeta {
  val path = PimpPath.fromRaw(Library decode folder)

  def toValues = Some((id, title, artist, album, duration.toSeconds.toInt, size.toBytes, folder))
}

object DataTrack {
  def fromValues(i: String, ti: String, ar: String, al: String, du: Int, si: Long, fo: String) =
    DataTrack(i, ti, ar, al, du.seconds, si.bytes, fo)

  implicit val durJson = JsonFormats.durationFormat
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val format = Json.format[DataTrack]
  implicit val dataResult = GetResult(r => DataTrack(r.<<, r.<<, r.<<, r.<<, r.nextInt().seconds, r.nextLong().bytes, r.<<))
}
