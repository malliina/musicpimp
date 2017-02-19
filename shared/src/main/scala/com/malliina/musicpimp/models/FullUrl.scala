package com.malliina.musicpimp.models

import java.util.regex.Pattern

import com.malliina.play.json.ValidatingCompanion
import play.api.mvc.{Call, RequestHeader}

case class FullUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def absolute(call: Call): FullUrl = {
    val fragment = Option(call.fragment).filter(_.trim.nonEmpty).map(f => s"#$f") getOrElse ""
    val callUri = s"${call.url}$fragment"
    FullUrl(proto, hostAndPort, callUri)
  }

  def +(more: String) = append(more)

  def append(more: String) = FullUrl(proto, hostAndPort, s"$uri$more")

  override def toString: String = url
}

object FullUrl extends ValidatingCompanion[String, FullUrl] {
  val urlPattern = Pattern compile """(.+)://([^/]+)(/?.*)"""

  def apply(call: Call, request: RequestHeader) =
    hostOnly(request).absolute(call)

  /** Ignores the uri of `request`.
    *
    * @param request source
    * @return a url of the host component of `request`
    */
  def hostOnly(request: RequestHeader): FullUrl = {
    val maybeS = if (request.secure) "s" else ""
    FullUrl(s"http$maybeS", request.host, "")
  }

  override def build(input: String): Option[FullUrl] = {
    val m = urlPattern.matcher(input)
    if (m.find() && m.groupCount() == 3) {
      Option(FullUrl(m group 1, m group 2, m group 3))
    } else {
      None
    }
  }

  override def write(t: FullUrl): String = t.url
}
