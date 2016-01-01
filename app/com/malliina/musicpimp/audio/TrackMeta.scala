package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.storage.StorageSize
import play.api.libs.json.Json._
import play.api.libs.json.{Format, Reads, Writes}

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
  implicit val sto = JsonFormats.storageSizeFormat
  implicit val dur = JsonFormats.durationFormat

  val trackWriter = Writes[TrackMeta](o => obj(
    ID -> o.id,
    TITLE -> o.title,
    ARTIST -> o.artist,
    ALBUM -> o.album,
    DURATION -> o.duration,
    SIZE -> o.size
  ))
  val reader: Reads[TrackMeta] = BaseTrackMeta.jsonFormat.map(base => {
    val meta: TrackMeta = base
    meta
  })
  implicit val format: Format[TrackMeta] = Format(reader, trackWriter)
}

