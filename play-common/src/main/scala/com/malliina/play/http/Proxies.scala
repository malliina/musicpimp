package com.malliina.play.http

import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc.{Headers, RequestHeader}

import scala.util.Try

/** I got false negatives with the default implementation of `request.secure` in Play,
  * so this object provides an alternative implementation in `Proxies.isSecure`.
  */
object Proxies {
  val Http = "http"
  val Https = "https"

  val CFVisitor = "CF-Visitor"
  val Scheme = "scheme"

  /** Call me instead of `request.secure`.
    *
    * @param rh request
    * @return true if the requests seems to use SSL, false otherwise
    */
  def isSecure(rh: RequestHeader): Boolean =
    rh.secure || hasSecureHeaders(rh.headers)

  def hasPlainHeaders(headers: Headers): Boolean =
    proto(headers) contains Http

  def hasSecureHeaders(headers: Headers): Boolean =
    proto(headers) contains Https

  /**
    * @param headers request headers
    * @return the raw protocol value, based on `headers` alone
    */
  def proto(headers: Headers): Option[String] =
    cloudFlareProto(headers) orElse xForwardedProto(headers)

  /** Example CF-Visitor value: {"scheme":"https"} or {"scheme":"http"}.
    *
    * @param headers request headers
    * @return the scheme, if any
    */
  def cloudFlareProto(headers: Headers): Option[String] =
    for {
      visitor <- headers.get(CFVisitor)
      json <- Try(Json.parse(visitor)).toOption
      proto <- (json \ Scheme).validate[String].asOpt
    } yield proto

  def xForwardedProto(headers: Headers): Option[String] =
    headers.get(HeaderNames.X_FORWARDED_PROTO)

  def realAddress(rh: RequestHeader): String =
    rh.headers.get(HeaderNames.X_FORWARDED_FOR) getOrElse rh.remoteAddress
}
