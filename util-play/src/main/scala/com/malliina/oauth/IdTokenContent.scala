package com.malliina.oauth

import com.malliina.values.{Email, ErrorMessage, JsonCompanion}
import play.api.libs.json.{Format, JsError, JsSuccess, Json, OFormat, Reads, Writes}

case class IdTokenContent(iss: String, sub: String, email: Email, aud: String, iat: Long, exp: Long)

object IdTokenContent {
  implicit val emailJson: Format[Email] = from(Email)
  implicit val json: OFormat[IdTokenContent] = Json.format[IdTokenContent]

  def from[T](comp: JsonCompanion[String, T]) = fromMapping(comp.build, comp.write)

  def fromMapping[T](
    read: String => Either[ErrorMessage, T],
    write: T => String
  ): Format[T] =
    Format(
      Reads.StringReads.flatMapResult(s =>
        read(s).fold(err => JsError(err.message), ok => JsSuccess(ok))
      ),
      Writes.StringWrites.contramap(write)
    )
}
