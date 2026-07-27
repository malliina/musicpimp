package com.malliina.musicpimp.auth

import org.http4s.{Headers, Request}
import org.typelevel.ci.CIStringSyntax

object Proxies2:
  val Http = "http"
  val Https = "https"

  val CFVisitor = ci"CF-Visitor"
  val Scheme = "scheme"
  val X_FORWARDED_FOR = ci"X-Forwarded-For"
  val X_FORWARDED_PROTO = ci"X-Forwarded-Proto"

  /** Call me instead of `request.secure`.
    *
    * @param rh
    *   request
    * @return
    *   true if the requests seems to use SSL, false otherwise
    */
  def isSecure(req: Request[?]): Boolean =
    req.isSecure.contains(true) || hasSecureHeaders(req.headers)

  def hasPlainHeaders(headers: Headers): Boolean =
    proto(headers) contains Http

  def hasSecureHeaders(headers: Headers): Boolean =
    proto(headers) contains Https

  /** @param headers
    *   request headers
    * @return
    *   the raw protocol value, based on `headers` alone
    */
  def proto(headers: Headers): Option[String] =
    cloudFlareProto(headers) orElse xForwardedProto(headers)

  /** Example CF-Visitor value: {"scheme":"https"} or {"scheme":"http"}.
    *
    * @param headers
    *   request headers
    * @return
    *   the scheme, if any
    */
  def cloudFlareProto(headers: Headers): Option[String] =
    for
      raw <- headers.get(CFVisitor)
      json <- io.circe.parser.parse(raw.head.value).toOption
      proto <- json.hcursor.downField(Scheme).as[String].toOption
    yield proto

  def xForwardedProto(headers: Headers): Option[String] =
    headers.get(X_FORWARDED_PROTO).map(_.head.value)

  def realAddress(req: Request[?]): String =
    realAddress(req.headers)
      .orElse(req.remoteAddr.map(_.toUriString))
      .getOrElse("")

  def realAddress(hs: Headers): Option[String] =
    hs
      .get(X_FORWARDED_FOR)
      .map(_.head.value)
