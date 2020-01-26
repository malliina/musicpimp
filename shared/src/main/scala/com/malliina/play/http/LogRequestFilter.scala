package com.malliina.play.http

import play.api.Logger
import play.api.mvc.{EssentialAction, EssentialFilter}

import scala.concurrent.ExecutionContext

object LogRequestFilter {
  def apply(ec: ExecutionContext): LogRequestFilter = new LogRequestFilter()(ec)
}

class LogRequestFilter()(implicit ec: ExecutionContext) extends EssentialFilter {
  private val log = Logger(getClass)

  override def apply(next: EssentialAction): EssentialAction = EssentialAction { req =>
    val headers = stringify(req.headers.toSimpleMap)
    log.info(s"${req.method} ${req.uri} $headers")
    next(req).map { r =>
      log.info(s"${req.method} ${req.uri} ${r.header.status} ${stringify(r.header.headers)}")
      r
    }
  }

  private def stringify(map: Map[String, String]) = map.map { case (k, v) => s"$k=$v" }
    .mkString(", ")
}
