package com.malliina.http

class ResponseException(val response: HttpResponse, val url: FullUrl)
  extends MusicException(s"Request to $url failed. Invalid response code ${response.code}. Body ${response.asString}.") {
  def code = response.code

  def body = response.asString
}

class CoverNotFoundException(msg: String) extends MusicException(msg)

class MusicException(msg: String) extends Exception(msg)
