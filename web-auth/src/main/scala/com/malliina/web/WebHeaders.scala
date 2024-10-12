package com.malliina.web

object WebHeaders extends WebHeaders

trait WebHeaders {
  val Accept = "Accept"
  val Authorization = "Authorization"
  val CacheControl = "Cache-Control"
  val ContentType = "Content-Type"
}

object HttpConstants extends HttpConstants

trait HttpConstants {
  val AudioMpeg = "audio/mpeg"
  val FormUrlEncoded = "application/x-www-form-urlencoded"
  val Json = "application/json"
  val NoCache = "no-cache"
  val NoCacheRevalidate = "no-cache, no-store, must-revalidate"
}
