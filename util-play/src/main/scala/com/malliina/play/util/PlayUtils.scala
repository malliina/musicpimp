package com.malliina.play.util

import com.malliina.play.util.PlayUtils.log
import play.api.Logger
import play.api.mvc.RequestHeader

trait PlayUtils {
  def formatHeaders(req: RequestHeader) =
    req.headers.toMap
      .map {
        case (key, values) => s"$key : ${values.mkString(",")}"
      }
      .mkString("\n", "\n", "")

  def logHeaders(req: RequestHeader) = log info formatHeaders(req)
}

object PlayUtils extends PlayUtils {
  private val log = Logger(getClass)
}
