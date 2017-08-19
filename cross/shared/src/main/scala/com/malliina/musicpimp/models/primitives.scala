package com.malliina.musicpimp.models

import play.api.libs.json.{Format, Json, Reads, Writes}

case class SimplePath(path: String) extends BasePath

trait BasePath {
  def path: String
}

object BasePath extends JsonCompanion[String, BasePath] {
  override def apply(raw: String): BasePath = SimplePath(raw)

  override def raw(t: BasePath) = t.path
}

case class RequestIdentifier(id: String) extends Ident

object RequestIdentifier extends IdentCompanion[RequestIdentifier]

case class TrackIdentifier(id: String) extends TrackIdent

object TrackIdentifier extends IdentCompanion[TrackIdentifier]

trait TrackIdent extends Ident {
  def id: String
}

object TrackIdent {
  implicit val json = Format[TrackIdent](
    _.validate[TrackIdentifier](TrackIdentifier.format),
    id => Json.toJson(TrackIdentifier(id.id))
  )
}


case class CloudName(id: String) extends Ident

object CloudName extends IdentCompanion[CloudName]

trait Ident {
  def id: String

  override def toString: String = id
}

abstract class IdentCompanion[T <: Ident] extends JsonCompanion[String, T] {
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
