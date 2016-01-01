package com.malliina.musicpimp.cloud

import play.api.libs.json.Json

/**
 * @author Michael
 */
case class RegistrationEvent(event: String, id: String) extends PimpMessages.PimpMessage

object RegistrationEvent {
  implicit val format = Json.format[RegistrationEvent]
}