package com.malliina.musicpimp.models

import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}

sealed trait CloudCommand

case class Connect(id: CloudID) extends CloudCommand derives Codec.AsObject

case object Disconnect extends CloudCommand

case object Noop extends CloudCommand

object CloudCommand:
  val CmdKey = "cmd"
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val SubscribeCmd = "subscribe"

  val reader = Decoder[CloudCommand]: json =>
    json
      .downField(CmdKey)
      .as[String]
      .flatMap:
        case ConnectCmd    => Decoder[Connect].decodeJson(json.value)
        case DisconnectCmd => Right(Disconnect)
        case SubscribeCmd  => Right(Noop)
        case other         => Left(DecodingFailure(s"Unknown '$CmdKey' value: '$other'.", Nil))
  val writer = Encoder[CloudCommand]:
    case c: Connect => simpleObj(ConnectCmd).deepMerge(Encoder[Connect].apply(c))
    case Disconnect => simpleObj(DisconnectCmd)
    case Noop       => simpleObj(SubscribeCmd)
  implicit val json: Codec[CloudCommand] = Codec.from[CloudCommand](reader, writer)

  private def simpleObj(cmd: String) = Json.obj(CmdKey -> cmd.asJson)
