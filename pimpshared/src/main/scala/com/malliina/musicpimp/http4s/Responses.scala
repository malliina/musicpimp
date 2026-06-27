package com.malliina.musicpimp.http4s

import cats.Applicative
import cats.data.NonEmptyList
import cats.implicits.toFunctorOps
import com.malliina.http.Errors
import com.malliina.http4s.BasicService.noCache
import com.malliina.musicpimp.auth.JsonInstances
import com.malliina.musicpimp.json.MediaRanges
import com.malliina.musicpimp.models.FailReason
import io.circe.Encoder
import org.http4s.{Challenge, EntityEncoder, Headers, MediaType, Request, Response, Status, Uri}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.{Accept, Location, `WWW-Authenticate`}

trait Responses[F[_]: Applicative] extends Http4sDsl[F] with JsonInstances:
  val genericMessage = "Something went wrong."
  val accessDeniedMessage = "Access denied."
  val badGatewayMessage = "A dependent server failed."

  val JsonKey = "json"

  def ok[A](a: A)(using EntityEncoder[F, A]) = Ok(a, noCache)

  def seeOther(uri: Uri): F[Response[F]] =
    SeeOther(Location(uri)).map(_.putHeaders(noCache))

  def badGatewayDefault: F[Response[F]] = badGateway(badGatewayMessage)

  def badGateway(message: String): F[Response[F]] = BadGateway(FailReason(message))

  def accessDenied: F[Response[F]] = unauthorizedNoCache(FailReason(accessDeniedMessage))

  def badRequest(errors: Errors): F[Response[F]] = badRequest(errors.message.message)

  def badRequest(message: String): F[Response[F]] = BadRequest(FailReason(message))

  def notFound(message: String): F[Response[F]] = NotFound(FailReason(message))

  def internalGeneric: F[Response[F]] = internal(genericMessage)

  def serverError: F[Response[F]] = internal("Server error.")

  def internal(message: String): F[Response[F]] = InternalServerError(FailReason(message))

  def notAcceptable(message: String): F[Response[F]] = NotAcceptable(FailReason(message))

  def unauthorizedNoCache[T: Encoder](errors: T): F[Response[F]] =
    Unauthorized(
      `WWW-Authenticate`(NonEmptyList.of(Challenge("Bearer", "Log in"))),
      errors,
      noCache
    )

  def pimpResult(
    request: Request[?]
  )(html: => F[Response[F]], json: => F[Response[F]]): F[Response[F]] =
    val mediaType = requestedResponseFormat(request)
    if mediaType.contains(MediaType.text.html) then html
    else if mediaType.exists(_.subType.contains(JsonKey)) then json
    else notAcceptable("Please use the 'Accept' header.")

  private def requestedResponseFormat(req: Request[?]): Option[MediaType] =
    val rs = ranges(req.headers)
    val qp = req.uri.query.params
    val jsonByQuery = qp.get("f").contains("json")
    if jsonByQuery then Some(MediaRanges.latest)
    else if rs.exists(_.satisfies(MediaType.text.html)) then Option(MediaType.text.html)
    else if rs.exists(_.satisfies(MediaRanges.anyJson)) then Option(MediaRanges.latest)
    else if rs.exists(_.satisfies(MediaRanges.JSONv17)) then Option(MediaRanges.JSONv17)
    else if rs.exists(r =>
        r.satisfies(MediaRanges.JSONv18) || r.satisfies(MediaType.application.json)
      )
    then Option(MediaRanges.JSONv18)
    else None

  private def ranges(headers: Headers) = headers
    .get[Accept]
    .map(_.values.map(_.mediaRange).toList)
    .getOrElse(Nil)
