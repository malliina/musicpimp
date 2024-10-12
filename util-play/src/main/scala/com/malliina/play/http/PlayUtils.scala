package com.malliina.play.http

import play.api.mvc.RequestHeader

trait PlayUtils {
  val lineSep = sys.props("line.separator")

  def headersString(request: RequestHeader) =
    request.headers.toSimpleMap
      .map(kv => kv._1 + "=" + kv._2)
      .mkString(lineSep, lineSep, "")
}

object PlayUtils extends PlayUtils
