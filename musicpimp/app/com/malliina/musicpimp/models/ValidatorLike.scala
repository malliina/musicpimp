package com.malliina.musicpimp.models

import play.api.libs.json._

abstract class ValidatorLike[In, T](implicit r: Format[In])
  extends ValidatingCompanion[In, T] {
  def apply(in: In): T

  def build(input: In): Option[T] =
    if (isValid(input)) Option(apply(input))
    else None

  def isValid(in: In): Boolean = true
}
