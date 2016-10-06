package com.malliina.musicpimp.models

import java.util.regex.Pattern

import play.api.mvc.{Call, RequestHeader}

case class PimpUrl(proto: String, hostAndPort: String, uri: String) {
  val host = hostAndPort.takeWhile(_ != ':')
  val protoAndHost = s"$proto://$hostAndPort"
  val url = s"$protoAndHost$uri"

  def absolute(call: Call): PimpUrl = {
    val fragment = Option(call.fragment) filter (_.trim.nonEmpty) map (f => s"#$f") getOrElse ""
    val callUri = s"${call.url}$fragment"
    PimpUrl(proto, hostAndPort, callUri)
  }

  override def toString: String = url
}

object PimpUrl extends ValidatingCompanion[String, PimpUrl] {
  val urlPattern = Pattern compile """(.+)://([^/]+)(/?.*)"""

  def apply(call: Call, request: RequestHeader) =
    hostOnly(request).absolute(call)

  /** Ignores the uri of `request`.
    *
    * @param request source
    * @return a url of the host component of `request`
    */
  def hostOnly(request: RequestHeader): PimpUrl = {
    val maybeS = if (request.secure) "s" else ""
    PimpUrl(s"http$maybeS", request.host, "")
  }

  override def build(input: String): Option[PimpUrl] = {
    val m = urlPattern.matcher(input)
    if (m.find() && m.groupCount() == 3) {
      Option(PimpUrl(m group 1, m group 2, m group 3))
    } else {
      None
    }
  }

  override def write(t: PimpUrl): String = t.url
}
