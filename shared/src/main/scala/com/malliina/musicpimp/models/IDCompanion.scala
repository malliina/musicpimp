package com.malliina.musicpimp.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat
import slick.jdbc.H2Profile.api.{MappedColumnType, stringColumnType}

import scala.reflect.ClassTag

abstract class IDCompanion[T <: Identifier : ClassTag] extends SimpleCompanion[String, T] {
  override def raw(t: T): String = t.id

  implicit val databaseMapping = MappedColumnType.base[T, String](raw, apply)
}
