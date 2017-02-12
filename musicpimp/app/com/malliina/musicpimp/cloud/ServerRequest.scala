package com.malliina.musicpimp.cloud

import java.util.UUID

import com.malliina.json.JsonFormats.SimpleFormat
import com.malliina.musicpimp.cloud.PimpMessages.PimpMessage
import play.api.libs.json.Json

case class ServerRequest(cmd: String, id: String, requestID: UUID, server: ServerInfo) extends PimpMessage {
  val request: String = requestID.toString
}

object ServerRequest {
  implicit val uuidFormat = new SimpleFormat[UUID](UUID.fromString)
  implicit val format = Json.format[ServerRequest]
}
