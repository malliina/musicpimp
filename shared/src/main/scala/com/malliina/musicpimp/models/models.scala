package com.malliina.musicpimp.models

import java.util.UUID

import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.play.json.ValidatingCompanion
import com.malliina.play.{ContentRange, Writeables}
import play.api.libs.json.Json

case class Version(version: String) extends PimpMessage

object Version {
  implicit val json = Json.format[Version]
  implicit val html = Writeables.fromJson[Version]
}

case class FailReason(reason: String) extends PimpMessage

object FailReason {
  implicit val json = Json.format[FailReason]
  implicit val html = Writeables.fromJson[FailReason]
}

case class WrappedID(id: String)

object WrappedID {
  implicit val json = Json.format[WrappedID]

  def forId(id: Identifiable): WrappedID = WrappedID(id.id)
}

case class WrappedLong(id: Long)

object WrappedLong {
  implicit val json = Json.format[WrappedLong]
}

case class RangedRequest(id: TrackID, range: ContentRange)

object RangedRequest {
  implicit val json = Json.format[RangedRequest]
}

sealed trait RequestID extends Identifiable

object RequestID extends ValidatingCompanion[String, RequestID] {
  private def apply(validated: String) = Impl(validated)

  override def build(input: String): Option[RequestID] =
    if (input.nonEmpty) Option(RequestID(input)) else None

  override def write(t: RequestID) = t.id

  def random(): RequestID = RequestID(UUID.randomUUID().toString)

  private case class Impl(id: String) extends RequestID
}
