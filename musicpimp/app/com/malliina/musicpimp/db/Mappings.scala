package com.malliina.musicpimp.db

import java.time.Instant

import com.malliina.play.models.Username
import slick.jdbc.H2Profile.api.{MappedColumnType, longColumnType, stringColumnType}

object Mappings {
  implicit val instant = MappedColumnType.base[Instant, Long](_.toEpochMilli, Instant.ofEpochMilli)
  implicit val username = MappedColumnType.base[Username, String](Username.raw, Username.apply)
}
