package com.malliina.musicpimp.models

import play.api.libs.json.{Format, Json, Reads, Writes}

case class RequestIdentifier(id: String) extends Identifier

object RequestIdentifier extends IdentCompanion[RequestIdentifier]

case class CloudName(id: String) extends Identifier

object CloudName extends IdentCompanion[CloudName]

trait Identifier {
  def id: String

  override def toString: String = id
}

abstract class IdentCompanion[T <: Identifier] extends JsonCompanion[String, T] {
  override def raw(t: T): String = t.id
}

abstract class JsonCompanion[Raw: Format, T] {
  def apply(raw: Raw): T

  def raw(t: T): Raw

  implicit val format = Format(
    Reads[T](in => in.validate[Raw].map(apply)),
    Writes[T](t => Json.toJson(raw(t)))
  )
}
