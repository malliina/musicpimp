package com.malliina.play.json

import play.api.data.format.Formatter
import play.api.data.{Forms, Mapping}
import play.api.libs.json.{Format, Json, Reads, Writes}
import play.api.mvc.PathBindable

abstract class SimpleCompanion[Raw, T](implicit
  rawBindable: PathBindable[Raw],
  jsonFormat: Format[Raw],
  formFormat: Formatter[Raw]
):
  def apply(raw: Raw): T

  def raw(t: T): Raw

  implicit val format: Format[T] = Format(
    Reads[T](in => in.validate[Raw].map(apply)),
    Writes[T](t => Json.toJson(raw(t)))
  )

  implicit val bindable: PathBindable[T] = rawBindable.transform(apply, raw)

  val mapping: Mapping[T] = Forms.of[Raw].transform(apply, raw)
