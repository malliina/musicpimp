package com.malliina.musicpimp.models

import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.PathBindable

abstract class SimpleCompanion[Raw, T](implicit rawBindable: PathBindable[Raw], f: Format[Raw]) {
  def apply(raw: Raw): T

  def raw(t: T): Raw

  implicit val format = Format(
    Reads[T](in => in.validate[Raw].map(apply)),
    Writes[T](t => Json.toJson(raw(t)))
  )

  implicit val bindable: PathBindable[T] = rawBindable.transform(apply, raw)
}
