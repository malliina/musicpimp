package com.malliina.musicpimp.db

import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.UnixPath
import play.api.libs.json.Json
import slick.jdbc.GetResult

import scala.concurrent.duration.{Duration, DurationInt}

case class DataTrack(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  duration: Duration,
  size: StorageSize,
  path: UnixPath,
  folder: FolderID
) extends TrackMeta {
//  def toValues = Some((id, title, artist, album, duration.toSeconds.toInt, size.toBytes, path, folder))
}

object DataTrack {
  def fromValues(
    i: TrackID,
    ti: String,
    ar: String,
    al: String,
    du: Int,
    si: Long,
    path: UnixPath,
    fo: FolderID
  ) =
    DataTrack(i, ti, ar, al, du.seconds, si.bytes, path, fo)

  implicit val durJson = PrimitiveFormats.durationFormat
  implicit val format = Json.format[DataTrack]
  implicit val dataResult: GetResult[DataTrack] = GetResult { r =>
    def readString(s: String) = r.rs.getString(s)

    DataTrack(
      TrackID(readString("ID")),
      readString("TITLE"),
      readString("ARTIST"),
      readString("ALBUM"),
      r.rs.getInt("DURATION").seconds,
      r.rs.getLong("SIZE").bytes,
      UnixPath.fromRaw(readString("PATH")),
      FolderID(readString("FOLDER"))
    )
  }
}
