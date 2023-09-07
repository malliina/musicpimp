package com.malliina.musicpimp.models

import java.util.UUID
import com.malliina.json.ValidatingCompanion
import com.malliina.musicpimp.cloud.PimpMessage
import com.malliina.play.{ContentRange, Writeables}
import play.api.http.Writeable
import play.api.libs.json.{Json, OFormat}

case class Version(version: String) extends PimpMessage

object Version {
  implicit val json: OFormat[Version] = Json.format[Version]
  implicit val html: Writeable[Version] = Writeables.fromJson[Version]
}

case class FailReason(reason: String) extends PimpMessage

object FailReason {
  implicit val json: OFormat[FailReason] = Json.format[FailReason]
  implicit val html: Writeable[FailReason] = Writeables.fromJson[FailReason]
}

case class WrappedID(id: String)

object WrappedID {
  implicit val json: OFormat[WrappedID] = Json.format[WrappedID]

  def forId(id: Identifier): WrappedID = WrappedID(id.id)
}

case class WrappedLong(id: Long)

object WrappedLong {
  implicit val json: OFormat[WrappedLong] = Json.format[WrappedLong]
}

case class RangedRequest(id: TrackID, range: ContentRange)

object RangedRequest {
  implicit val json: OFormat[RangedRequest] = Json.format[RangedRequest]
}

sealed trait RequestID extends Identifier {
  def toId = RequestIdentifier(id)
}

object RequestID extends ValidatingCompanion[String, RequestID] {
  private def apply(validated: String) = Impl(validated)

  override def build(input: String): Option[RequestID] =
    if (input.nonEmpty) Option(RequestID(input)) else None

  override def write(t: RequestID) = t.id

  def random(): RequestID = RequestID(UUID.randomUUID().toString)

  private case class Impl(id: String) extends RequestID

}

case class SimpleCommand(cmd: String)

object SimpleCommand {
  implicit val json: OFormat[SimpleCommand] = Json.format[SimpleCommand]
}
