package com.malliina.play.auth

import com.malliina.play.auth.Authenticator.Outcome
import com.malliina.play.concurrent.FutureUtils
import com.malliina.play.models.AuthRequest
import com.malliina.values.Username
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

trait UserAuthenticator extends Authenticator[Username] {
  def withRequest()(implicit ec: ExecutionContext): Authenticator[AuthRequest] =
    transform[AuthRequest]((req, user) => Right(new AuthRequest(user, req)))
}

object UserAuthenticator {
  // Can I somehow reduce the repetition with Authenticator.apply?
  def apply(auth: RequestHeader => Future[Outcome[Username]]): UserAuthenticator =
    new UserAuthenticator {
      override def authenticate(rh: RequestHeader): Future[Outcome[Username]] =
        auth(rh)
    }

  def default(
    isValid: BasicCredentials => Future[Option[Username]]
  )(implicit ec: ExecutionContext): Authenticator[Username] =
    Authenticator.anyOne(session(), header(isValid), query(isValid))

  def session(key: String = Auth.DefaultSessionKey): UserAuthenticator =
    apply { rh =>
      val outcome = Auth.authenticateFromSession(rh, key).toRight(MissingCredentials(rh))
      Future.successful(outcome)
    }

  /** Basic HTTP authentication.
    *
    * The "Authorization" request header should be like: "Basic base64(username:password)", where
    * base64(x) means x base64-encoded.
    *
    * @param isValid validator of credentials
    * @return the username wrapped in an Option if successfully authenticated, None otherwise
    */
  def header(isValid: BasicCredentials => Future[Option[Username]])(implicit ec: ExecutionContext) =
    basic(Auth.basicCredentials, isValid)

  /** Authenticates based on the "u" and "p" query string parameters.
    *
    * @param isValid validator
    * @return the username, if successfully authenticated
    */
  def query(isValid: BasicCredentials => Future[Option[Username]])(implicit ec: ExecutionContext) =
    basic(rh => Auth.credentialsFromQuery(rh), isValid)

  def basic(
    read: RequestHeader => Option[BasicCredentials],
    isValid: BasicCredentials => Future[Option[Username]]
  )(implicit ec: ExecutionContext): UserAuthenticator =
    UserAuthenticator { rh =>
      read(rh)
        .map { creds =>
          isValid(creds).map { maybeUser =>
            maybeUser
              .map { user =>
                Right(user)
              }
              .getOrElse {
                Left(InvalidCredentials(rh))
              }
          }
        }
        .getOrElse {
          Future.successful(Left(MissingCredentials(rh)))
        }
    }

  def readCreds(rh: RequestHeader): Option[BasicCredentials] =
    Auth.basicCredentials(rh) orElse Auth.credentialsFromQuery(rh)
}

/**
  * @tparam T type of successful auth, for example a username
  */
trait Authenticator[+T] {
  def authenticate(rh: RequestHeader): Future[Outcome[T]]

  def map[U](f: T => U)(implicit ec: ExecutionContext): Authenticator[U] =
    transform((_, t) => Right(f(t)))

  def mapAuth[U](f: (RequestHeader, T) => U)(implicit ec: ExecutionContext) =
    transform((req, t) => Right(f(req, t)))

  def transform[U](
    f: (RequestHeader, T) => Outcome[U]
  )(implicit ec: ExecutionContext): Authenticator[U] =
    Authenticator[U] { rh =>
      authenticate(rh).map { outcome =>
        outcome.fold(
          failure => Left(failure),
          t => f(rh, t)
        )
      }
    }
}

object Authenticator {
  type Outcome[+T] = Either[AuthFailure, T]

  def apply[T](auth: RequestHeader => Future[Outcome[T]]): Authenticator[T] =
    new Authenticator[T] {
      override def authenticate(rh: RequestHeader) = auth(rh)
    }

  def negative[T]: Authenticator[T] = apply { rh =>
    Future.successful(Left(InvalidCredentials(rh)))
  }

  def anyOne[T](auths: Authenticator[T]*)(implicit ec: ExecutionContext): Authenticator[T] = {
    val authList = auths.toList
    Authenticator[T] { rh =>
      FutureUtils.first(authList)(_.authenticate(rh))(_.isRight)
    }
  }
}
