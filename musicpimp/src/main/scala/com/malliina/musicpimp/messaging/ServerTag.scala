package com.malliina.musicpimp.messaging

import com.malliina.values.{ErrorMessage, ValidatingCompanion}

case class ServerTag(tag: String)

object ServerTag extends ValidatingCompanion[String, ServerTag]:
  override def build(input: String): Either[ErrorMessage, ServerTag] = Right(apply(input))
  override def write(t: ServerTag): String = t.tag
