package com.malliina.musicpimp.auth

import cats.Functor
import cats.effect.Sync
import cats.syntax.all.toFunctorOps
import com.malliina.musicpimp.auth.Authenticator.AuthOutcome
import com.malliina.play.auth.{BasicCredentials, Token}
import com.malliina.play.concurrent.FutureUtils
import io.circe.Json
import org.http4s.Request

case class AuthedRequest[F[_]](user: UserPayload, request: Request[F], token: Option[Token] = None):
  def username = user.username

case class JsonRequest[F[_]](user: UserPayload, request: Request[F], body: Json, token: Option[Token] = None):
  def username = user.username

type UserAuthenticator[F[_]] = Authenticator[F, UserPayload]

object UserAuthenticator:
  def default[F[_]: Sync](preferred: UserAuthenticator[F])(
    isValid: BasicCredentials => F[Option[UserPayload]]
  ): UserAuthenticator[F] =
    anyOne(preferred, header[F](isValid), query[F](isValid))

  def header[F[_]: Sync](
    isValid: BasicCredentials => F[Option[UserPayload]]
  ): UserAuthenticator[F] =
    basic[F](req => Auth2.basicCredentials(req.headers), creds => isValid(creds))

  def query[F[_]: Sync](isValid: BasicCredentials => F[Option[UserPayload]]) =
    basic[F](req => Auth2.credentialsFromQuery(req.uri), creds => isValid(creds))

  def basic[F[_]: Sync](
    read: Request[F] => Option[BasicCredentials],
    isValid: BasicCredentials => F[Option[UserPayload]]
  ): UserAuthenticator[F] =
    Authenticator.make[F, UserPayload]: req =>
      read(req)
        .map[F[AuthOutcome[UserPayload]]]: creds =>
          isValid(creds).map: maybeUser =>
            maybeUser
              .map: user =>
                Right(user)
              .getOrElse:
                Left(InvalidCredentials(req))
        .getOrElse:
          Sync[F].pure[AuthOutcome[UserPayload]](Left(MissingCredentials(req)))

  def anyOne[F[_]: Sync](auths: UserAuthenticator[F]*): UserAuthenticator[F] =
    Authenticator.make[F, UserPayload]: req =>
      FutureUtils.firstIO(auths.toList)(_.authenticate(req))(_.isRight)

trait SyncAuthenticator[F[_]: Sync, T] extends Authenticator[F, T]:
  def auth(req: Request[F]): AuthOutcome[T]
  override def authenticate(req: Request[F]): F[AuthOutcome[T]] = Sync[F].pure(auth(req))

trait Authenticator[F[_]: Functor, T]:
  def authenticate(req: Request[F]): F[AuthOutcome[T]]

  def map[U](f: T => U): Authenticator[F, U] =
    transform((_, t) => Right(f(t)))

  def mapAuth[U](f: (Request[F], T) => U) =
    transform((req, t) => Right(f(req, t)))

  def transform[U](f: (Request[F], T) => AuthOutcome[U]): Authenticator[F, U] =
    Authenticator.make[F, U]: (req: Request[F]) =>
      authenticate(req).map: outcome =>
        outcome.fold(
          failure => Left(failure),
          t => f(req, t)
        )

object Authenticator:
  type AuthOutcome[+T] = Either[Http4sAuthFailure, T]

  def io[F[_]: Functor, T](auth: Request[F] => F[AuthOutcome[T]]): Authenticator[F, T] =
    make(req => auth(req))

  def make[F[_]: Functor, T](auth: Request[F] => F[AuthOutcome[T]]): Authenticator[F, T] =
    new Authenticator[F, T]:
      override def authenticate(req: Request[F]): F[AuthOutcome[T]] = auth(req)

  def negative[F[_]: Sync, T]: Authenticator[F, T] = make: rh =>
    Sync[F].pure(Left(InvalidCredentials(rh)))

  def anyOne[F[_]: Sync, T](auths: Authenticator[F, T]*): Authenticator[F, T] =
    val authList = auths.toList
    Authenticator.make[F, T]: rh =>
      FutureUtils.firstIO(authList)(_.authenticate(rh))(_.isRight)

sealed trait Http4sAuthFailure:
  def req: Request[?]

case class InvalidCredentials(req: Request[?]) extends Http4sAuthFailure
case class MissingCredentials(req: Request[?]) extends Http4sAuthFailure
case class InvalidCookie(req: Request[?]) extends Http4sAuthFailure
case class MissingCookie(req: Request[?]) extends Http4sAuthFailure
