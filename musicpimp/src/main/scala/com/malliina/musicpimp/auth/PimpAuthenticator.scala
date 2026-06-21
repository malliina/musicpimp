package com.malliina.musicpimp.auth

import cats.Functor
import cats.implicits.toFunctorOps
import com.malliina.values.{Password, Username}
import org.http4s.Request

class PimpAuthenticator[F[_]: Functor](
  val userManager: UserManager[F, Username, Password],
  val rememberMe: RememberMe[F]
) extends CookieAuthenticator[F]:
  val cookie = PimpAuthenticator.cookie(rememberMe)

  override def authenticate(user: Username, pass: Password): F[Boolean] =
    userManager.authenticate(user, pass)

  override def authenticateFromCookie(
    req: Request[F]
  ): F[Either[Http4sAuthFailure, AuthedRequest[F]]] =
    cookie.authenticate(req)

object PimpAuthenticator:
  def cookie[F[_]: Functor](rememberMe: RememberMe[F]): Authenticator[F, AuthedRequest[F]] =
    Authenticator.io[F, AuthedRequest[F]]: req =>
      rememberMe
        .authenticate(req)
        .map: either =>
          either.map: token =>
            AuthedRequest(UserPayload(token.user), req, Option(token))
