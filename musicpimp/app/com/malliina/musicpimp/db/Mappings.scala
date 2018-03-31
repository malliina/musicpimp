package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.play.models.Username
import com.malliina.values.UnixPath
import slick.jdbc.JdbcProfile

object Mappings {
  def apply(profile: JdbcProfile) = new Mappings(profile)
}

class Mappings(val profile: JdbcProfile) {

  import profile.api.{MappedColumnType, longColumnType, stringColumnType}

  implicit val instant = MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)
  implicit val username = MappedColumnType.base[Username, String](Username.raw, Username.apply)
  implicit val trackId = MappedColumnType.base[TrackID, String](_.id, TrackID.apply)
  implicit val folderId = MappedColumnType.base[FolderID, String](_.id, FolderID.apply)
  implicit val unixPath = MappedColumnType.base[UnixPath, String](_.path, UnixPath.apply)
}
