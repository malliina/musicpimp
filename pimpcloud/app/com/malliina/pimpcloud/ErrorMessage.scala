package com.malliina.pimpcloud

import play.api.libs.json.Json

case class ErrorMessage(message: String)

object ErrorMessage {
  implicit val json = Json.format[ErrorMessage]
}
