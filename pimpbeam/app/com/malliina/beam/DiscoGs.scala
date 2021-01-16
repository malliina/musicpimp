package com.malliina.beam

import com.malliina.util.WebUtils.encodeURIComponent
import play.api.libs.ws.{WSClient, WSRequest}

class DiscoGs(http: WSClient) {
  def request(artist: String, album: String): WSRequest =
    http.url(coverUrl(artist, album))

  def coverUrl(artist: String, album: String): String = {
    val artistEnc = encodeURIComponent(artist)
    val albumEnc = encodeURIComponent(album)
    s"https://api.musicpimp.org/covers?artist=$artistEnc&album=$albumEnc"
  }

  def close(): Unit = http.close()
}
