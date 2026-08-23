package com.malliina.auth

import com.malliina.values.Username
import io.circe.Codec

case class Token(user: Username, series: Long, token: Long) derives Codec.AsObject:
  val asUnAuth = UnAuthToken(user.name, series, token)
