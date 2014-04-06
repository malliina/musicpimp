package com.mle.play.json

import play.api.libs.json.{JsResult, JsValue, Format}
import scala.concurrent.duration.Duration
import play.api.libs.json.Json._
import com.mle.storage.StorageSize
import com.mle.storage.StorageLong
import concurrent.duration.DurationLong

/**
 *
 * @author mle
 */
trait JsonFormats2 {

  /**
   * Serializes Duration to Long (seconds), deserializes Long to Duration.
   */
  implicit object durationFormat extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Long].map(_.seconds)
  }

  implicit object storageSizeFormat extends Format[StorageSize] {
    override def writes(o: StorageSize): JsValue = toJson(o.toBytes)

    override def reads(json: JsValue): JsResult[StorageSize] =
      json.validate[Long].map(_.bytes)
  }

  /**
   * Json reader/writer. Writes toString and reads as specified by `f`.
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

object JsonFormats2 extends JsonFormats2