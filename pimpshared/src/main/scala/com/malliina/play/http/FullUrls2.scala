package com.malliina.play.http

import com.malliina.http.FullUrl
import com.malliina.musicpimp.auth.Proxies2
import org.http4s.{Request, Uri}
import org.typelevel.ci.CIStringSyntax

object FullUrls2:
  def absolute(url: FullUrl, uri: Uri): FullUrl =
    FullUrl(url.proto, url.hostAndPort, uri.renderString)

  def hostOnly2[F[_]](req: Request[F]): FullUrl =
    val maybeS = if Proxies2.isSecure(req) then "s" else ""
    val hostFromHeader = req.headers.get(ci"Host").map(_.head.value)
    FullUrl(s"http$maybeS", req.uri.host.map(_.value).orElse(hostFromHeader).getOrElse(""), "")

  def isSecure[F[_]](req: Request[F]): Boolean =
    req.isSecure.getOrElse(false) || req.headers
      .get(ci"X-Forwarded-Proto")
      .exists(_.exists(_.value == "https"))
