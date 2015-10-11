package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import com.mle.storage.StorageSize
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Writes}

import scala.concurrent.duration.Duration

trait TrackMeta {
  def id: String

  def title: String

  def artist: String

  def album: String

  def duration: Duration

  def size: StorageSize
}

object TrackMeta {
//  val format = (
//    (JsPath \ ID).format[String] and
//      (JsPath \ TITLE).format[String] and
//      (JsPath \ ARTIST).format[String] and
//      (JsPath \ ALBUM).format[String] and
//      (JsPath \ DURATION).format[Duration] and
//      (JsPath \ SIZE).format[StorageSize]
//    )(apply, unlift(unapply))

  implicit val trackWriter = new Writes[TrackMeta] {
    def writes(o: TrackMeta): JsValue = obj(
      ID -> o.id,
      TITLE -> o.title,
      ARTIST -> o.artist,
      ALBUM -> o.album,
      DURATION -> o.duration.toSeconds,
      SIZE -> o.size.toBytes
    )
  }
}
