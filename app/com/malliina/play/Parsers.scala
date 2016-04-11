package com.malliina.play

import play.api.mvc.BodyParsers.parse
import play.api.mvc.{AnyContentAsEmpty, Headers}
import play.mvc.Http.HeaderNames.{CONTENT_LENGTH, TRANSFER_ENCODING}

import scala.util.Try

/** Workaround for Play 2.5.1's inability to handle POSTs with empty bodies.
  *
  * Once Play 2.5.2 is out, we can test and re-evaluate whether this is still needed.
  *
  * @see https://github.com/playframework/playframework/issues/5972
  * @see https://github.com/playframework/playframework/commit/c0f1270c8584e90d512474eb9b2b87af754a89f8
  */
object Parsers {
  val default = parse.using { request =>
    if (hasBody(request.headers)) parse.anyContent
    else parse.ignore(AnyContentAsEmpty)
  }

  def hasBody(headers: Headers) =
    headers.get(CONTENT_LENGTH).exists(len => Try(len.toLong).filter(_ > 0).isSuccess) ||
      headers.get(TRANSFER_ENCODING).isDefined
}
