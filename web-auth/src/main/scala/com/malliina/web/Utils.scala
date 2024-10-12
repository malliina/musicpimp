package com.malliina.web

import java.math.BigInteger
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.SecureRandom

object Utils {
  private val rng = new SecureRandom()

  def randomString(): String = new BigInteger(130, rng).toString(32)

  def urlEncode(s: String) = URLEncoder.encode(s, StandardCharsets.UTF_8.name())

  def stringify(map: Map[String, String]): String =
    map.map { case (key, value) => s"$key=$value" }.mkString("&")
}
