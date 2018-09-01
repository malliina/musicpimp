package com.malliina.play

import com.malliina.musicpimp.auth.UserManager
import com.malliina.play.auth.{AuthFailure, Authenticator, RememberMe}
import com.malliina.play.http.AuthedRequest
import com.malliina.values.{Password, Username}
import play.api.mvc.RequestHeader

import scala.concurrent.{ExecutionContext, Future}

class PimpAuthenticator(val userManager: UserManager[Username, Password],
                        val rememberMe: RememberMe,
                        ec: ExecutionContext) extends CookieAuthenticator {
  implicit val exec = ec
  val cookie = PimpAuthenticator.cookie(rememberMe)

  override def authenticate(user: Username, pass: Password): Future[Boolean] =
    userManager.authenticate(user, pass)

  override def authenticateFromCookie(rh: RequestHeader): Future[Either[AuthFailure, AuthedRequest]] =
    cookie.authenticate(rh)
}

object PimpAuthenticator {
  def cookie(rememberMe: RememberMe)(implicit ec: ExecutionContext) =
    Authenticator[AuthedRequest] { rh =>
      rememberMe.authenticate(rh).map { either =>
        either.right.map { token =>
          AuthedRequest(token.user, rh, Option(rememberMe.cookify(token)))
        }
      }
    }
}
