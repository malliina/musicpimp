package com.mle.play

import com.mle.musicpimp.models.User
import com.mle.play.controllers.AuthResult
import play.api.mvc.RequestHeader

import scala.concurrent.Future

/**
  * @author mle
  */
trait Authenticator {
  /**
    * @param user username
    * @param pass password
    * @return true if the credentials are valid, false otherwise
    */
  def authenticate(user: User, pass: String): Future[Boolean]

  def authenticateFromCookie(req: RequestHeader): Future[Option[AuthResult]]
}
