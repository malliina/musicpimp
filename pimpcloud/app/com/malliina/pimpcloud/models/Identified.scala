package com.malliina.pimpcloud.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat

abstract class Identified[T <: Identifiable] extends SimpleCompanion[String, T] {
  override def raw(t: T): String = t.id
}
