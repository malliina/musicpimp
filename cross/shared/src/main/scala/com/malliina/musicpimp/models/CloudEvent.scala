package com.malliina.musicpimp.models

import com.malliina.musicpimp.js.{CloudStrings, FrontStrings}
import com.malliina.musicpimp.json.CommonMessages
import play.api.libs.json._

sealed abstract class CloudEvent(event: String)

object CloudEvent extends CloudStrings with CommonMessages {
  val reader = Reads[CloudEvent] { json =>
    (json \ FrontStrings.EventKey).validate[String].flatMap {
      case ConnectedKey => (json \ IdKey).validate[CloudID].map { id => Connected(id) }
      case DisconnectingKey => JsSuccess(Disconnecting)
      case ConnectingKey => JsSuccess(Connecting)
      case DisconnectedKey => (json \ Reason).validate[String].map { r => Disconnected(r) }
      case other => JsError(s"Unknown ${FrontStrings.EventKey} value: '$other'.")
    }
  }
  val writer: Writes[CloudEvent] = Writes[CloudEvent] {
    case Connected(id) => event(ConnectedKey, IdKey -> id)
    case Disconnected(reason) => event(DisconnectedKey, Reason -> reason)
    case Connecting => event(ConnectingKey)
    case Disconnecting => event(DisconnectingKey)
  }
  implicit val json: Format[CloudEvent] = Format(reader, writer)

  case class Connected(id: CloudID) extends CloudEvent(ConnectedKey)

  case class Disconnected(reason: String) extends CloudEvent(DisconnectedKey)

  case object Connecting extends CloudEvent(ConnectingKey)

  case object Disconnecting extends CloudEvent(DisconnectingKey)

}
