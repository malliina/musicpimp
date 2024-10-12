package com.malliina.play.http

import play.api.mvc.RequestHeader

import scala.util.Try

object ServerUtils {
  def portFromHost(req: RequestHeader): Option[Int] = {
    val maybeSuffix = req.host.dropWhile(_ != ':')
    if (maybeSuffix.length > 1) Try(maybeSuffix.tail.toInt).toOption
    else None
  }
}
