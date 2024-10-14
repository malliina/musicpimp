package com.malliina.musicpimp.db

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.CrossFormats.finiteDuration
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath
import io.circe.Codec
import io.getquill.Embedded
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
  derives Codec.AsObject
