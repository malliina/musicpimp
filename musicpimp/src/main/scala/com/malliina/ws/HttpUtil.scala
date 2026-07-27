package com.malliina.ws

import java.util.Base64

object HttpUtil:
  val Authorization = "Authorization"
  val Basic = "Basic"
  val Utf8 = "UTF-8"

  def authorizationValue(username: String, password: String) =
    s"$Basic " + Base64.getEncoder.encodeToString((username + ":" + password).getBytes(Utf8))
