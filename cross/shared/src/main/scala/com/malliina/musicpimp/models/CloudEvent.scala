package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats.{evented, singleEvent}
import play.api.libs.json._

sealed abstract class CloudEvent

case class Connected(id: CloudID) extends CloudEvent

object Connected {
  val Key = "connected"
  implicit val json: OFormat[Connected] = evented(Key, Json.format[Connected])
}

case class Disconnected(reason: String) extends CloudEvent

object Disconnected {
  val Key = "disconnected"
  implicit val json: OFormat[Disconnected] = evented(Key, Json.format[Disconnected])
}

case object Connecting extends CloudEvent {
  val Key = "connecting"
  implicit val json: OFormat[Connecting.type] = singleEvent(Key, Connecting)
}

case object Disconnecting extends CloudEvent {
  val Key = "disconnecting"
  implicit val json: OFormat[Disconnecting.type] = singleEvent(Key, Disconnecting)
}

object CloudEvent {
  val reader = Reads[CloudEvent] { json =>
    Connected.json.reads(json)
      .orElse(Disconnected.json.reads(json))
      .orElse(Connecting.json.reads(json))
      .orElse(Disconnecting.json.reads(json))
  }
  val writer: Writes[CloudEvent] = Writes[CloudEvent] {
    case e: Connected => Connected.json.writes(e)
    case e: Disconnected => Disconnected.json.writes(e)
    case Connecting => Connecting.json.writes(Connecting)
    case Disconnecting => Disconnecting.json.writes(Disconnecting)
  }
  implicit val json: Format[CloudEvent] = Format(reader, writer)
}
