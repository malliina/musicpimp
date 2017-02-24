package com.malliina.play

import com.malliina.musicpimp.auth.UserManager
import com.malliina.play.auth.RememberMe
import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.{Password, Username}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class PimpAuthenticator(val userManager: UserManager[Username, Password],
                        val rememberMe: RememberMe) extends CookieAuthenticator {
  override def authenticate(user: Username, pass: Password): Future[Boolean] =
    userManager.authenticate(user, pass)

  override def authenticateFromCookie(req: RequestHeader): Future[Option[AuthedRequest]] =
    rememberMe.authenticateFromCookie(req)
}
