package com.malliina.web

import java.text.ParseException
import java.time.Instant

import com.malliina.http.{ResponseError, ResponseException, StatusError}
import com.malliina.values.{ErrorMessage, TokenValue}
import play.api.libs.json.{JsError, JsPath, JsonValidationError}

import scala.concurrent.duration.{Duration, DurationLong}

sealed abstract class AuthError(val key: String) {
  def message: ErrorMessage
}

object JsonError {
  def apply(err: Seq[(JsPath, Seq[JsonValidationError])]): JsonError =
    apply(JsError(err))

  def apply(message: String): JsonError =
    JsonError(JsError(message))
}

case class OkError(error: ResponseError) extends AuthError("http_error") {
  override def message: ErrorMessage = ErrorMessage(error match {
    case StatusError(r, _)                    => s"Status code ${r.code}."
    case com.malliina.http.JsonError(_, _, _) => "JSON error."
    case _                                    => "Unknown error"
  })
}

object OkError {
  def apply(e: ResponseException): OkError = apply(e.error)
}

case class PermissionError(message: ErrorMessage) extends AuthError("permission_error")

case class OAuthError(message: ErrorMessage) extends AuthError("oauth_error")
object OAuthError {
  def apply(s: String): OAuthError = OAuthError(ErrorMessage(s))
}

case class JsonError(err: JsError) extends AuthError("json_error") {
  override def message = ErrorMessage(s"JSON error. $err")
}

sealed abstract class JWTError(key: String) extends AuthError(key) {
  def token: TokenValue
  def message: ErrorMessage
}

case class Expired(token: TokenValue, exp: Instant, now: Instant) extends JWTError("token_expired") {
  def since: Duration = (now.toEpochMilli - exp.toEpochMilli).millis
  override def message = ErrorMessage(s"Token expired $since ago, at $exp.")
}

case class NotYetValid(token: TokenValue, nbf: Instant, now: Instant) extends JWTError("not_yet_valid") {
  def validIn = (nbf.toEpochMilli - now.toEpochMilli).millis

  override def message = ErrorMessage(
    s"Token not yet valid. Valid in $validIn. Valid from $nbf, checked at $now."
  )
}

case class IssuerMismatch(token: TokenValue, actual: Issuer, allowed: Seq[Issuer]) extends JWTError("issuer_mismatch") {
  def message = ErrorMessage(
    s"Issuer mismatch. Got '$actual', but expected one of '${allowed.mkString(", ")}'."
  )
}

case class InvalidSignature(token: TokenValue) extends JWTError("invalid_signature") {
  override def message = ErrorMessage("Invalid JWT signature.")
}

case class InvalidKeyId(token: TokenValue, kid: String, expected: Seq[String]) extends JWTError("invalid_kid") {
  def message = ErrorMessage(
    s"Invalid key ID. Expected one of '${expected.mkString(", ")}', but got '$kid'."
  )
}

case class InvalidClaims(token: TokenValue, message: ErrorMessage) extends JWTError("invalid_claims")

case class ParseError(token: TokenValue, e: ParseException) extends JWTError("parse_error") {
  override def message = ErrorMessage("Parse error")
}

case class MissingData(token: TokenValue, message: ErrorMessage) extends JWTError("missing_data")
