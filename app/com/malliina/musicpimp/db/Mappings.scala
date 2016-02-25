package com.malliina.musicpimp.db

import org.joda.time.DateTime

import scala.slick.driver.H2Driver.simple.{MappedColumnType, longColumnType}

object Mappings {
  implicit val jodaDate = MappedColumnType.base[DateTime, Long](_.getMillis, l => new DateTime(l))
}
