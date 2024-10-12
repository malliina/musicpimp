package com.malliina.play.auth

import com.malliina.values.{Password, Username}
import org.apache.commons.codec.binary.Base64
import play.api.http.HeaderNames.AUTHORIZATION
import play.api.mvc.RequestHeader

object Auth {
  val DefaultScheme = "Bearer"
  val DefaultSessionKey = "username"

  def basicCredentials(request: RequestHeader): Option[BasicCredentials] = {
    authHeaderParser(request) { decoded =>
      decoded.split(":", 2) match {
        case Array(user, pass) => Some(BasicCredentials(Username(user), Password(pass)))
        case _                 => None
      }
    }
  }

  def readAuthToken(rh: RequestHeader, scheme: String = DefaultScheme): Option[String] =
    rh.headers.get(AUTHORIZATION).flatMap { authInfo =>
      authInfo.split(" ") match {
        case Array(name, value) if name.toLowerCase == scheme.toLowerCase =>
          Option(value)
        case _ =>
          None
      }
    }

  /**
    *
    * @param request
    * @param f decoded credentials => T
    * @tparam T
    * @return
    */
  def authHeaderParser[T](request: RequestHeader)(f: String => Option[T]): Option[T] = {
    request.headers.get(AUTHORIZATION) flatMap { authInfo =>
      authInfo.split(" ") match {
        case Array(_, encodedCredentials) =>
          val decoded = new String(Base64.decodeBase64(encodedCredentials.getBytes))
          f(decoded)
        case _ =>
          None
      }
    }
  }

  def credentialsFromQuery(
    request: RequestHeader,
    userKey: String = "u",
    passKey: String = "p"
  ): Option[BasicCredentials] = {
    val qString = request.queryString
    for (u <- qString get userKey;
         p <- qString get passKey;
         user <- u.headOption;
         pass <- p.headOption) yield BasicCredentials(Username(user), Password(pass))
  }

  def authenticateFromSession(
    rh: RequestHeader,
    sessionKey: String = DefaultSessionKey
  ): Option[Username] =
    rh.session.get(sessionKey).map(Username.apply)
}
