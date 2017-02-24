package com.malliina.play

import com.malliina.play.http.AuthedRequest
import com.malliina.play.models.{Password, Username}
import play.api.mvc.RequestHeader

import scala.concurrent.Future

trait CookieAuthenticator {
  /**
    * @param user username
    * @param pass password
    * @return true if the credentials are valid, false otherwise
    */
  def authenticate(user: Username, pass: Password): Future[Boolean]

  def authenticateFromCookie(req: RequestHeader): Future[Option[AuthedRequest]]
}
