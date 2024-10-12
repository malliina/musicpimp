package com.malliina.util

import java.net.URLEncoder

trait WebUtils {

  /**
    * Simulates JavaScript.encodeURIComponent(...)
    *
    * http://stackoverflow.com/questions/607176/java-equivalent-to-javascripts-encodeuricomponent-that-produces-identical-outpu
    */
  def encodeURIComponent(input: String) =
    URLEncoder
      .encode(input, "UTF-8")
      .replaceAll("\\+", "%20")
      .replaceAll("\\%21", "!")
      .replaceAll("\\%27", "'")
      .replaceAll("\\%28", "(")
      .replaceAll("\\%29", ")")
      .replaceAll("\\%7E", "~")
}

object WebUtils extends WebUtils
