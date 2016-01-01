package com.malliina.play

import com.malliina.musicpimp.models.User
import com.malliina.play.controllers.AuthResult
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
