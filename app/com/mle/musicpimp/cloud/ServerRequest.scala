package com.mle.musicpimp.cloud

import java.util.UUID

import com.mle.json.JsonFormats.SimpleFormat
import com.mle.musicpimp.cloud.PimpMessages.PimpMessage
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class ServerRequest(cmd: String, id: String, requestID: UUID, server: ServerInfo) extends PimpMessage {
  val request: String = requestID.toString
}

object ServerRequest {
  implicit val uuidFormat = new SimpleFormat[UUID](UUID.fromString)
  implicit val format = Json.format[ServerRequest]
}