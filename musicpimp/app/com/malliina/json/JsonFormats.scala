package com.malliina.json

import play.api.libs.json.Json._
import play.api.libs.json.{Format, JsResult, JsValue}

trait JsonFormats {

  /** Json reader/writer. Writes toString and reads as specified by `f`.
    *
    * @param reader maps a name to the type
    * @tparam T type of element
    */
  class SimpleFormat[T](reader: String => T) extends Format[T] {
    def reads(json: JsValue): JsResult[T] =
      json.validate[String].map(reader)

    def writes(o: T): JsValue = toJson(o.toString)
  }

}

object JsonFormats extends JsonFormats
