package com.malliina.musicpimp.auth

import cats.effect.Sync
import cats.implicits.toFunctorOps
import com.malliina.values.{Password, Username}
import org.http4s.Request

trait CookieAuthenticator[F[_]]:

  /** @param user
    *   username
    * @param pass
    *   password
    * @return
    *   true if the credentials are valid, false otherwise
    */
  def authenticate(user: Username, pass: Password): F[Boolean]

  def authenticateFromCookie(req: Request[F]): F[Either[Http4sAuthFailure, AuthedRequest[F]]]

object CookieAuthenticator:
  def default[F[_]: Sync](
    session: UserAuthenticator[F],
    auth: CookieAuthenticator[F]
  ): Authenticator[F, AuthedRequest[F]] =
    bundle[F](session, auth).transform((rh, user) => Right(AuthedRequest(user, rh)))

  def bundle[F[_]: Sync](
    session: UserAuthenticator[F],
    auth: CookieAuthenticator[F]
  ): UserAuthenticator[F] =
    UserAuthenticator.default[F](session): creds =>
      auth
        .authenticate(creds.username, creds.password)
        .map: isValid =>
          if isValid then Option(UserPayload(creds.username)) else None
