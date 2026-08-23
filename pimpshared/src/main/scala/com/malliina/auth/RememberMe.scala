package com.malliina.auth

import com.malliina.values.Password

/** Adapted from https://github.com/wsargent/play20-rememberme
  */
object RememberMe:
  val CookieName = "REMEMBER_ME"
  val SeriesName = "series"
  val UserIdName = "userId"
  val TokenName = "token"

case class PasswordChange(oldPass: Password, newPass: Password, newPassAgain: Password)
