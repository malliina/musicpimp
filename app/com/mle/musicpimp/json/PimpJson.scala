package com.mle.musicpimp.json

import play.api.libs.json.{JsResult, JsValue, Format}
import play.api.libs.json.Json._
import concurrent.duration._
import com.mle.audio.PlayerStates

/**
 *
 * @author mle
 */
trait PimpJson {
  /**
   * Serializes Duration to Long, deserializes Double to Duration.
   */
  implicit object durationFormat extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Double].map(_.seconds)
  }

  implicit object playStateFormat extends SimpleFormat[PlayerStates.PlayerState](PlayerStates.withName)

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

object PimpJson extends PimpJson
