package com.malliina.play

import com.malliina.play.auth.{AuthFailure, Authenticator, UserAuthenticator}
import com.malliina.play.http.AuthedRequest
import com.malliina.values.{Password, Username}
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

trait CookieAuthenticator:

  /** @param user
    *   username
    * @param pass
    *   password
    * @return
    *   true if the credentials are valid, false otherwise
    */
  def authenticate(user: Username, pass: Password): Future[Boolean]

  def authenticateFromCookie(req: RequestHeader): Future[Either[AuthFailure, AuthedRequest]]

object CookieAuthenticator:
  def default(
    auth: CookieAuthenticator
  )(implicit ec: ExecutionContext): Authenticator[AuthedRequest] =
    bundle(auth).transform((rh, user) => Right(AuthedRequest(user, rh)))

  def bundle(auth: CookieAuthenticator)(implicit ec: ExecutionContext) =
    UserAuthenticator.default: creds =>
      auth
        .authenticate(creds.username, creds.password)
        .map: isValid =>
          if isValid then Option(creds.username) else None
