package com.malliina.musicpimp.json

import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.storage.{StorageLong, StorageSize}
import play.api.libs.json.Json.toJson
import play.api.libs.json._

import scala.concurrent.duration.{Duration, DurationLong, FiniteDuration}

object CrossFormats {

  /** Serializes Duration to Long (seconds), deserializes Long to Duration.
    */
  implicit object duration extends Format[Duration] {
    def writes(o: Duration): JsValue = toJson(o.toSeconds)

    def reads(json: JsValue): JsResult[Duration] =
      json.validate[Long].map(_.seconds)
  }

  implicit val finiteDuration: Format[FiniteDuration] = Format[FiniteDuration](
    Reads(_.validate[Long].map(_.seconds)),
    Writes(d => toJson(d.toSeconds))
  )

  implicit val storageSize: Format[StorageSize] = Format[StorageSize](
    Reads(_.validate[Long].map(_.bytes)),
    size => toJson(size.toBytes)
  )

  def singleEvent[T](value: String, t: T): OFormat[T] = evented(value, pure(t))

  def evented[T](eventName: String, payload: OFormat[T]): OFormat[T] =
    keyValued(EventKey, eventName, payload)

  def singleCmd[T](value: String, t: T) = cmd(value, pure(t))

  def cmd[T](value: String, payload: OFormat[T]): OFormat[T] =
    keyValued(CommonStrings.Cmd, value, payload)

  def pure[T](value: T): OFormat[T] = OFormat(_ => JsSuccess(value), (_: T) => Json.obj())

  /** A JSON format for objects of type T that contains a top-level key-value pair.
    */
  def keyValued[T](key: String, value: String, payload: OFormat[T]): OFormat[T] = {
    val reader: Reads[T] = Reads { json =>
      (json \ key)
        .validate[String]
        .filter(_ == value)
        .flatMap(_ => payload.reads(json))
    }
    val writer = OWrites[T] { t =>
      Json.obj(key -> value) ++ payload.writes(t)
    }
    OFormat(reader, writer)
  }
}
