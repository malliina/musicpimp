package com.malliina.play.auth

import com.malliina.values.Username

case class UnAuthToken(user: Username, series: Long, token: Long) {
  lazy val isEmpty = this == UnAuthToken.empty
}

object UnAuthToken {
  val empty = UnAuthToken(Username.empty, 0, 0)
}
