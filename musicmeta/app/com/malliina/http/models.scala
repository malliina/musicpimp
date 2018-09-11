package com.malliina.http

import play.api.libs.json.Json

case class SingleError(message: String)

object SingleError {
  implicit val json = Json.format[SingleError]
}

case class Errors(errors: Seq[SingleError])

object Errors {
  implicit val json = Json.format[Errors]
}
