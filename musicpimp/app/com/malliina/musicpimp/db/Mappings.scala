package com.malliina.musicpimp.db

import com.malliina.play.models.Username
import org.joda.time.DateTime
import slick.driver.H2Driver.api.{MappedColumnType, longColumnType, stringColumnType}

object Mappings {
  implicit val jodaDate = MappedColumnType.base[DateTime, Long](_.getMillis, l => new DateTime(l))
  implicit val username = MappedColumnType.base[Username, String](Username.raw, Username.apply)
}
