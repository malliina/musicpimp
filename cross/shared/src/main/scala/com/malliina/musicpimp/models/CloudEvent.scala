package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats.{evented, singleEvent}
import io.circe.{Codec, Decoder, Encoder}
import io.circe.generic.semiauto.deriveCodec
import play.api.libs.json.*

sealed abstract class CloudEvent

case class Connected(id: CloudID) extends CloudEvent

object Connected:
  val Key = "connected"
  implicit val json: Codec[Connected] = evented(Key, deriveCodec[Connected])

case class Disconnected(reason: String) extends CloudEvent

object Disconnected:
  val Key = "disconnected"
  implicit val json: Codec[Disconnected] = evented(Key, deriveCodec[Disconnected])

case object Connecting extends CloudEvent:
  val Key = "connecting"
  implicit val json: Codec[Connecting.type] = singleEvent(Key, Connecting)

case object Disconnecting extends CloudEvent:
  val Key = "disconnecting"
  implicit val json: Codec[Disconnecting.type] = singleEvent(Key, Disconnecting)

object CloudEvent:
  val reader = Decoder[CloudEvent]: json =>
    val v = json.value
    Connected.json
      .decodeJson(v)
      .orElse(Disconnected.json.decodeJson(v))
      .orElse(Connecting.json.decodeJson(v))
      .orElse(Disconnecting.json.decodeJson(v))
  val writer: Encoder[CloudEvent] = Encoder[CloudEvent]:
    case e: Connected    => Connected.json(e)
    case e: Disconnected => Disconnected.json(e)
    case Connecting      => Connecting.json(Connecting)
    case Disconnecting   => Disconnecting.json(Disconnecting)
  implicit val json: Codec[CloudEvent] = Codec.from(reader, writer)
