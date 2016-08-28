package com.malliina.play

import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.models.User
import com.malliina.play.auth.RememberMe
import com.malliina.play.http.AuthedRequest
import play.api.mvc.RequestHeader

import scala.concurrent.Future

class PimpAuthenticator(val userManager: UserManager[User, String],
                        val rememberMe: RememberMe) extends Authenticator {
  override def authenticate(user: User, pass: String): Future[Boolean] =
    userManager.authenticate(user, pass)

  override def authenticateFromCookie(req: RequestHeader): Future[Option[AuthedRequest]] =
    rememberMe.authenticateFromCookie(req)
}
