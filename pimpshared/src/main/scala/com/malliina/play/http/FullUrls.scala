package com.malliina.play.http

import com.malliina.http.FullUrl
import play.api.mvc.{Call, RequestHeader}

object FullUrls {
  def absolute(url: FullUrl, call: Call): FullUrl = {
    val fragment = Option(call.fragment)
      .filter(_.trim.nonEmpty)
      .map(f => s"#$f")
      .getOrElse("")
    val callUri = s"${call.url}$fragment"
    FullUrl(url.proto, url.hostAndPort, callUri)
  }

  def apply(call: Call, request: RequestHeader): FullUrl =
    absolute(hostOnly(request), call)

  /** Ignores the uri of `request`.
    *
    * @param rh source
    * @return a url of the host component of `request`
    */
  def hostOnly(rh: RequestHeader): FullUrl = {
    val maybeS = if (Proxies.isSecure(rh)) "s" else ""
    FullUrl(s"http$maybeS", rh.host, "")
  }
}
