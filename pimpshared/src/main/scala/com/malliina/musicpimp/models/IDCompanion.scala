package com.malliina.musicpimp.models

import com.malliina.values.JsonCompanion
import play.api.data.format.Formats.stringFormat
import play.api.data.{Forms, Mapping}

import scala.reflect.ClassTag

abstract class IDCompanion[T <: Identifier : ClassTag] extends JsonCompanion[String, T] {
  override def write(t: T): String = t.id

  implicit val form: Mapping[T] = Forms.of[String].transform(apply, write)
}
