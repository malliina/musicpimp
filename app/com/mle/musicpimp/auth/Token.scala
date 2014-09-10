package com.mle.musicpimp.auth

import controllers.UnAuthToken
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class Token(user: String, series: Long, token: Long) {
  val asUnAuth = UnAuthToken(user, series, token)
}

object Token {
  implicit val json = Json.format[Token]
}