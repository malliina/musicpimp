package com.malliina.pimpcloud

import com.malliina.play.Writeables
import play.api.libs.json.Json

case class ErrorResponse(errors: Seq[ErrorMessage])

object ErrorResponse {
  implicit val json = Json.format[ErrorResponse]
  implicit val writeable = Writeables.fromJson[ErrorResponse]

  def simple(message: String) = ErrorResponse(Seq(ErrorMessage(message)))
}
