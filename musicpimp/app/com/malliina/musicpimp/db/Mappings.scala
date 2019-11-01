package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID}
import com.malliina.storage.{StorageLong, StorageSize}
import com.malliina.values.{UnixPath, Username}
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

object Mappings {
  def apply(profile: JdbcProfile) = new Mappings(profile)
}

class Mappings(val profile: JdbcProfile) {

  import profile.api.{MappedColumnType, intColumnType, longColumnType, stringColumnType}

  implicit val instant = MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)
  implicit val username = MappedColumnType.base[Username, String](Username.write, Username.apply)
  implicit val trackId = MappedColumnType.base[TrackID, String](_.id, TrackID.apply)
  implicit val folderId = MappedColumnType.base[FolderID, String](_.id, FolderID.apply)
  implicit val unixPath = MappedColumnType.base[UnixPath, String](_.path, UnixPath.apply)
  implicit val durationMapping = MappedColumnType.base[Duration, Int](_.toSeconds.toInt, _.seconds)
  implicit val finiteDurationMapping =
    MappedColumnType.base[FiniteDuration, Int](_.toSeconds.toInt, _.seconds)
  implicit val storageSize = MappedColumnType.base[StorageSize, Long](_.toBytes, _.bytes)
  implicit val playlistIdMapping = MappedColumnType.base[PlaylistID, Long](_.id, PlaylistID.apply)
}
