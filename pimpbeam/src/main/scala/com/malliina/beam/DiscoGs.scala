package com.malliina.beam

import com.malliina.http.FullUrl
import com.malliina.http.UrlSyntax.url
import com.malliina.http.io.HttpClientF

class DiscoGs[F[_]](http: HttpClientF[F]):
  def request(artist: String, album: String) =
    http.get(coverUrl(artist, album))

  private def coverUrl(artist: String, album: String): FullUrl =
    val baseUrl = url"https://api.musicpimp.org/covers"
    baseUrl.query(Map("artist" -> artist, "album" -> album))
