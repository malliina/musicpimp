package com.malliina.http

import play.api.libs.json.{Json, OFormat}

case class SingleError(message: String)

object SingleError {
  implicit val json: OFormat[SingleError] = Json.format[SingleError]
}

case class Errors(errors: Seq[SingleError])

object Errors {
  implicit val json: OFormat[Errors] = Json.format[Errors]
}
