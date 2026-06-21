package com.malliina.play.http

import com.malliina.http.FullUrl
import com.malliina.musicpimp.auth.Proxies2
import org.http4s.{Request, Uri}
import play.api.mvc.RequestHeader

object FullUrls:
  def absolute(url: FullUrl, uri: Uri): FullUrl =
    FullUrl(url.proto, url.hostAndPort, uri.renderString)

  /** Ignores the uri of `request`.
    *
    * @param rh
    *   source
    * @return
    *   a url of the host component of `request`
    */
  def hostOnly(rh: RequestHeader): FullUrl =
    val maybeS = if Proxies.isSecure(rh) then "s" else ""
    FullUrl(s"http$maybeS", rh.host, "")

  def hostOnly2[F[_]](req: Request[F]): FullUrl =
    val maybeS = if Proxies2.isSecure(req) then "s" else ""
    FullUrl(s"http$maybeS", req.uri.host.map(_.value).getOrElse(""), "")
