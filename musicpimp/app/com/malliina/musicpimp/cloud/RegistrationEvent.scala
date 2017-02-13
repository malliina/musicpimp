package com.malliina.musicpimp.cloud

import play.api.libs.json.Json

case class RegistrationEvent(event: String, id: CloudID) extends PimpMessage

object RegistrationEvent {
  implicit val format = Json.format[RegistrationEvent]
}
