package com.malliina.musicpimp.db

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, PimpPath, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json
import slick.jdbc.GetResult

import scala.concurrent.duration.{Duration, DurationInt}

case class DataTrack(id: TrackID,
                     title: String,
                     artist: String,
                     album: String,
                     duration: Duration,
                     size: StorageSize,
                     folder: FolderID) extends TrackMeta {
  val path = PimpPath.fromRaw(Library decode folder.id)

  def toValues = Some((id, title, artist, album, duration.toSeconds.toInt, size.toBytes, folder))
}

object DataTrack {
  def fromValues(i: TrackID, ti: String, ar: String, al: String, du: Int, si: Long, fo: FolderID) =
    DataTrack(i, ti, ar, al, du.seconds, si.bytes, fo)

  implicit val durJson = JsonFormats.durationFormat
  implicit val storageJson = JsonFormats.storageSizeFormat
  implicit val format = Json.format[DataTrack]
  implicit val dataResult: GetResult[DataTrack] =
    GetResult(r => DataTrack(TrackID(r.<<), r.<<, r.<<, r.<<, r.nextInt().seconds, r.nextLong().bytes, FolderID(r.<<)))
}
