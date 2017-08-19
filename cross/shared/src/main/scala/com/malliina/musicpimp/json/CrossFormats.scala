package com.malliina.musicpimp.json

import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, JsResult, JsValue}

import scala.concurrent.duration.{Duration, DurationLong}

object CrossFormats {

  /** Serializes Duration to Long (seconds), deserializes Long to Duration.
    */
  implicit object durationFormat extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Long].map(_.seconds)
  }

}
