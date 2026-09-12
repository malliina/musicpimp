package com.malliina.musicpimp.auth

import com.malliina.auth.BasicUserPassCredentials
import com.malliina.values.{Password, Username}
import org.apache.commons.codec.binary.Base64
import org.http4s.{Headers, Uri}
import org.typelevel.ci.{CIString, CIStringSyntax}

object Auth2 extends Auth2

trait Auth2:
  val Authorization: CIString = ci"Authorization"

  def basicCredentials(headers: Headers): Option[BasicUserPassCredentials] =
    authHeaderParser(headers): decoded =>
      decoded.split(":", 2) match
        case Array(user, pass) =>
          val result = for
            user <- Username.build(user)
            pass <- Password.build(pass)
          yield BasicUserPassCredentials(user, pass)
          result.toOption
        case _ => None

  def authHeaderParser[T](headers: Headers)(f: String => Option[T]): Option[T] =
    headers
      .get(Authorization)
      .flatMap: authInfo =>
        authInfo.head.value.split(" ") match
          case Array(_, encodedCredentials) =>
            val decoded = new String(Base64.decodeBase64(encodedCredentials.getBytes))
            f(decoded)
          case _ =>
            None

  def credentialsFromQuery(
    request: Uri,
    userKey: String = "u",
    passKey: String = "p"
  ): Option[BasicUserPassCredentials] =
    val qString = request.query.params
    for
      user <- qString.get(userKey)
      pass <- qString.get(passKey)
      username <- Username.build(user).toOption
      password <- Password.build(pass).toOption
    yield BasicUserPassCredentials(username, password)
