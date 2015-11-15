package com.mle.play

import com.mle.musicpimp.auth.UserManager
import com.mle.musicpimp.models.User
import com.mle.play.auth.RememberMe
import com.mle.play.controllers.AuthResult
import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
  * @author mle
  */
class PimpAuthenticator(val userManager: UserManager, val rememberMe: RememberMe) extends Authenticator {
  override def authenticate(user: User, pass: String): Future[Boolean] = {
    userManager.authenticate(user.name, pass)
  }

  override def authenticateFromCookie(req: RequestHeader): Future[Option[AuthResult]] = {
    rememberMe.authenticateFromCookie(req)
  }
}
