package com.malliina.ws

import org.java_websocket.util.Base64

object HttpUtil {
  def authorizationValue(username: String, password: String) =
    "Basic " + Base64.encodeBytes((username + ":" + password).getBytes("UTF-8"))
}
