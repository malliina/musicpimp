package com.malliina.musicpimp.models

import play.api.libs.json._

sealed trait CloudCommand

case class Connect(id: CloudID) extends CloudCommand

object Connect {
  implicit val json = Json.format[Connect]
}

case object Disconnect extends CloudCommand

case object Noop extends CloudCommand

object CloudCommand {
  val CmdKey = "cmd"
  val ConnectCmd = "connect"
  val DisconnectCmd = "disconnect"
  val SubscribeCmd = "subscribe"

  val reader = Reads[CloudCommand] { json =>
    (json \ CmdKey).validate[String].flatMap {
      case ConnectCmd => json.validate[Connect]
      case DisconnectCmd => JsSuccess(Disconnect)
      case SubscribeCmd => JsSuccess(Noop)
      case other => JsError(s"Unknown '$CmdKey' value: '$other'.")
    }
  }
  val writer = Writes[CloudCommand] {
    case c: Connect => simpleObj(ConnectCmd) ++ Connect.json.writes(c)
    case Disconnect => simpleObj(DisconnectCmd)
    case Noop => simpleObj(SubscribeCmd)
  }
  implicit val json = Format[CloudCommand](reader, writer)

  def simpleObj(cmd: String): JsObject = Json.obj(CmdKey -> cmd)
}
