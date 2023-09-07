package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.models.CloudID
import play.api.libs.json.{Json, OFormat}

case class RegistrationEvent(event: String, id: CloudID) extends PimpMessage

object RegistrationEvent {
  implicit val format: OFormat[RegistrationEvent] = Json.format[RegistrationEvent]
}
