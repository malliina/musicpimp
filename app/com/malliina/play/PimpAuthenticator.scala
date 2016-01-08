package com.malliina.play

import com.malliina.musicpimp.auth.UserManager
import com.malliina.musicpimp.models.User
import com.malliina.play.auth.RememberMe
import com.malliina.play.controllers.AuthResult
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