package com.malliina.play

import play.api.data.format.Formatter
import play.api.data.{Forms, Mapping}
import play.api.libs.json.Format
import play.api.mvc.PathBindable
import play.api.data.format.Formats.stringFormat

import scala.reflect.ClassTag

abstract class PlayString[T: ClassTag] extends PlayCompanion[String, T]

abstract class PlayCompanion[Raw, T: ClassTag](implicit rawBindable: PathBindable[Raw],
                                               jsonFormat: Format[Raw],
                                               formFormat: Formatter[Raw]) {
  def apply(raw: Raw): T

  def raw(t: T): Raw

  implicit val bindable: PathBindable[T] = rawBindable.transform(apply, raw)

  val mapping: Mapping[T] = Forms.of[Raw].transform(apply, raw)
}
