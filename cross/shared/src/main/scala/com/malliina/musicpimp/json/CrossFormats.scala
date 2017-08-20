package com.malliina.musicpimp.json

import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json.toJson
import play.api.libs.json.{Format, JsResult, JsValue, Reads}

import scala.concurrent.duration.{Duration, DurationLong}

object CrossFormats {

  /** Serializes Duration to Long (seconds), deserializes Long to Duration.
    */
  implicit object duration extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Long].map(_.seconds)
  }

  implicit val storageSize: Format[StorageSize] = Format[StorageSize](
    Reads(_.validate[Long].map(_.bytes)),
    size => toJson(size.toBytes)
  )
}
