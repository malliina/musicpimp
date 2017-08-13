package com.malliina.musicpimp.models

import com.malliina.musicpimp.js.CloudStrings
import com.malliina.musicpimp.json.CommonMessages
import play.api.libs.json.Writes

sealed abstract class CloudEvent(event: String)

object CloudEvent extends CloudStrings with CommonMessages {
  implicit val writer: Writes[CloudEvent] = Writes[CloudEvent] {
    case Connected(id) => event(ConnectedKey, IdKey -> id)
    case Disconnected(reason) => event(DisconnectedKey, Reason -> reason)
    case Connecting => event(ConnectingKey)
    case Disconnecting => event(DisconnectingKey)
  }

  case class Connected(id: CloudId) extends CloudEvent(ConnectedKey)

  case class Disconnected(reason: String) extends CloudEvent(DisconnectedKey)

  case object Connecting extends CloudEvent(ConnectingKey)

  case object Disconnecting extends CloudEvent(DisconnectingKey)

}
