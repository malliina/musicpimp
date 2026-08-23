package com.malliina.musicpimp.auth

import cats.effect.Sync
import cats.implicits.{toFlatMapOps, toFunctorOps}
import com.malliina.musicpimp.auth.RememberMe.{CookieName, Unauth, log}
import com.malliina.auth.{Token, TokenStore}
import com.malliina.util.AppLogger
import com.malliina.values.Username
import io.circe.Codec
import org.http4s.Request

import scala.util.Random

object RememberMe:
  private val log = AppLogger(getClass)

  val CookieName = "REMEMBER_ME"

  case class Unauth(user: Username, series: Long, token: Long) derives Codec.AsObject

class RememberMe[F[_]: Sync](store: TokenStore[F], auth: Http4sAuth[F])
  extends Authenticator[F, Token]:
  def authenticate(req: Request[F]): F[Either[Http4sAuthFailure, Token]] =
    auth
      .read[Unauth](CookieName, req)
      .map: unauth =>
        cookieAuth(unauth, req)
      .getOrElse:
        log.debug(s"Found no token in request: ${req.cookies}")
        Sync[F].pure(Left(MissingCookie(req)))

  private def cookieAuth(
    attempt: Unauth,
    req: Request[F]
  ): F[Either[Http4sAuthFailure, Token]] =
    log.debug(s"Authenticating: $attempt")
    val user = attempt.user
    store
      .findToken(user, attempt.series)
      .flatMap: maybeToken =>
        maybeToken
          .map[F[Either[Http4sAuthFailure, Token]]]: savedToken =>
            if savedToken.token == attempt.token then

              /** I believe the intention is to ensure that a browser cannot reuse another browser's
                * token.
                *
                * The token is replaced with a new one at each successful token authentication,
                * while the series remains the same; this updated cookie is then sent to the
                * browser. The series acts as a browser identifier. So, if there's a token mismatch,
                * it suggests some other actor has authenticated using this browser's token, which
                * is suspect.
                */
              log.info(s"Cookie authentication succeeded. Updating token.")
              for
                _ <- store.remove(savedToken)
                newToken = Token(user, attempt.series, Random.nextLong())
                _ <- store.persist(newToken)
              yield Right(newToken)
            else
              log.warn(s"The saved token did not match the one from the request. Refusing access.")
              store.removeAll(user).map(_ => Left(InvalidCookie(req)))
          .getOrElse:
            log.debug(s"Unable to authenticate token: $attempt")
            Sync[F].pure(Left(InvalidCredentials(req)))
