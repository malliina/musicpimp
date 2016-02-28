package com.malliina.musicpimp.db

import org.joda.time.DateTime
import slick.driver.H2Driver.api.{MappedColumnType, longColumnType}

object Mappings {
  implicit val jodaDate = MappedColumnType.base[DateTime, Long](_.getMillis / 1000, l => new DateTime(l * 1000))
}
