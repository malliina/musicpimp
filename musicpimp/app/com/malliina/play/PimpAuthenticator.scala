package com.malliina.play

import cats.effect.IO
import com.malliina.musicpimp.auth.UserManager
import com.malliina.play.auth.{AuthFailure, Authenticator, RememberMe}
import com.malliina.play.http.AuthedRequest
import com.malliina.values.{Password, Username}
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

class PimpAuthenticator(
  val userManager: UserManager[IO, Username, Password],
  val rememberMe: RememberMe,
  ec: ExecutionContext
) extends CookieAuthenticator:
  implicit val exec: ExecutionContext = ec
  val cookie = PimpAuthenticator.cookie(rememberMe)

  override def authenticate(user: Username, pass: Password): IO[Boolean] =
    userManager.authenticate(user, pass)

  override def authenticateFromCookie(
    rh: RequestHeader
  ): Future[Either[AuthFailure, AuthedRequest]] =
    cookie.authenticate(rh)

object PimpAuthenticator:
  def cookie(rememberMe: RememberMe): Authenticator[AuthedRequest] =
    Authenticator.io[AuthedRequest]: rh =>
      rememberMe
        .authenticate(rh)
        .map: either =>
          either.map: token =>
            AuthedRequest(token.user, rh, Option(rememberMe.cookify(token)))
