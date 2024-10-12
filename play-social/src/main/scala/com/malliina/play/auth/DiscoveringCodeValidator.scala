package com.malliina.play.auth

import cats.effect.IO
import com.malliina.play.auth.DiscoveringCodeValidator.log
import com.malliina.play.http.FullUrls
import com.malliina.web.*
import play.api.Logger
import play.api.libs.json.Json
import play.api.mvc.Results.BadGateway
import play.api.mvc.{Call, RequestHeader, Result}

object DiscoveringCodeValidator:
  private val log = Logger(getClass)

/** A validator where the authorization and token endpoints are obtained through a discovery
  * endpoint ("knownUrl").
  *
  * @param codeConf
  *   conf
  * @tparam V
  *   type of authenticated user
  */
abstract class DiscoveringCodeValidator[V](
  codeConf: AuthCodeConf[IO],
  results: AuthResults[V],
  val redirCall: Call
) extends DiscoveringAuthFlow[IO, V](codeConf)
  with PlaySupport[Verified]
  with AuthValidator:

  override def onOutcome(outcome: Either[AuthError, Verified], req: RequestHeader): Result =
    results.resultFor(outcome.flatMap(parse), req)

  /** The initial result that initiates sign-in.
    */
  override def start(
    req: RequestHeader,
    extraParams: Map[String, String] = Map.empty
  ): IO[Result] =
    start(FullUrls(redirCall, req), extraParams)
      .map: s =>
        redirResult(s.authorizationEndpoint, s.params, s.nonce)
      .handleErrorWith: e =>
        log.error(s"HTTP error.", e)
        IO.pure(BadGateway(Json.obj("message" -> "HTTP error.")))
