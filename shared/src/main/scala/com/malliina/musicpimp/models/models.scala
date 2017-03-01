package com.malliina.musicpimp.models

import com.malliina.musicpimp.cloud.PimpMessage
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

  def apply(id: Identifiable): WrappedID = WrappedID(id.id)
}

case class WrappedLong(id: Long)

object WrappedLong {
  implicit val json = Json.format[WrappedLong]
}

case class RangedRequest(id: TrackID, range: ContentRange)

object RangedRequest {
  implicit val json = Json.format[RangedRequest]
}
