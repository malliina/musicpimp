package com.malliina.musicpimp.auth

import com.malliina.http.{Errors, SingleError}
import io.circe.{DecodingFailure, Json}
import org.http4s.*

class JsonException(val error: DecodingFailure, val json: Json) extends DecodeFailure:
  val errors = JsonException.errors(error)
  val detailed = errors.errors
    .filter(_.message.message.nonEmpty)
    .map(e => e.message)
    .mkString(", ")
  override def message: String = s"JSON exception for '$json'. $detailed"
  override def cause: Option[Throwable] = None
  override def toHttpResponse[F[_]](httpVersion: HttpVersion): Response[F] =
    Response(Status.BadRequest, httpVersion).withEntity(JsonException.errors(error))(using
      JsonInstances.jsonEncoderOf[F, Errors]
    )

object JsonException:
  def errors(error: DecodingFailure): Errors =
    Errors(SingleError.input(s"JSON error. $error"))

class InvalidRequest(val req: Request[?], val errors: Errors)
  extends Exception(s"Invalid request to '${req.uri}'. ${errors.message}."):
  def message = getMessage
