package com.mle.musicpimp.json

import play.api.libs.json.{JsResult, JsValue, Format}
import play.api.libs.json.Json._

/**
 *
 * @author mle
 */
class SimpleFormat[T](reader: String => T) extends Format[T] {
  def reads(json: JsValue): JsResult[T] =
    json.validate[String].map(reader)

  def writes(o: T): JsValue = toJson(o.toString)
}