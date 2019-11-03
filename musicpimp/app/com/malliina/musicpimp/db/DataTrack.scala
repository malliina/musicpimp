package com.malliina.musicpimp.db

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath
import io.getquill.Embedded
import play.api.libs.json.{Format, Json}

import scala.concurrent.duration.FiniteDuration

case class DataTrack(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  duration: FiniteDuration,
  size: StorageSize,
  path: UnixPath,
  folder: FolderID
) extends TrackMeta
  with Embedded

object DataTrack {
  implicit val fd: Format[FiniteDuration] = CrossFormats.finiteDuration
  implicit val format = Json.format[DataTrack]
}
