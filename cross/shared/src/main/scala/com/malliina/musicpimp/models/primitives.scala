package com.malliina.musicpimp.models

import com.malliina.values.JsonCompanion

case class RequestIdentifier(id: String) extends Identifier

object RequestIdentifier extends IdentCompanion[RequestIdentifier]

trait Identifier {
  def id: String

  override def toString: String = id
}

abstract class IdentCompanion[T <: Identifier] extends JsonCompanion[String, T] {
  override def write(t: T): String = t.id
}
