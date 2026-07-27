package com.malliina.musicpimp.auth

import com.malliina.config.ConfigReadable
import com.malliina.values.Username
import io.circe.Codec

case class SecretKey(value: String) extends AnyVal:
  override def toString = "****"

object SecretKey:
  val dev = SecretKey("app-jwt-signing-secret-goes-here-must-be-sufficiently-long")

  given ConfigReadable[SecretKey] = ConfigReadable.string.map(apply)

// Names of cookies
case class CookieConf(user: String, intendedUri: String)

case class UserPayload(username: Username) derives Codec.AsObject
