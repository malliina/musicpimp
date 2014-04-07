package com.mle.musicpimp.audio

import scala.concurrent.duration.Duration
import com.mle.storage.StorageSize
import play.api.libs.json.{JsValue, Writes}
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonStrings._

trait TrackMeta {
  def id: String

  def title: String

  def artist: String

  def album: String

  def duration: Duration

  def size: StorageSize
}

object TrackMeta {
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
