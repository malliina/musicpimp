package com.mle.push

import play.api.libs.json.Json

/**
 *
 * @author mle
 */
case class PushUrl(url: String, silent: Boolean, tag: String)

object PushUrl {
  implicit val json = Json.format[PushUrl]
}