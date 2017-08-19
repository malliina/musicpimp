package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats
import play.api.libs.json.{Format, Json, Reads}

import scala.concurrent.duration.Duration

case class SimpleRange(description: String) extends RangeLike

object SimpleRange {
  implicit val json = Json.format[SimpleRange]
}

trait RangeLike {
  def description: String
}

object RangeLike {
  implicit val json: Format[RangeLike] = Format[RangeLike](
    Reads[RangeLike](_.validate[SimpleRange]),
    r => Json.toJson(SimpleRange(r.description))
  )
}

trait MusicItem {
  def id: Ident

  def title: String
}

case class SimpleTrack(id: TrackIdent,
                       title: String,
                       album: String,
                       artist: String,
                       duration: Duration) extends BaseTrack

object SimpleTrack {
  implicit val durFormat = CrossFormats.durationFormat
  implicit val json = Json.format[SimpleTrack]
}

trait BaseTrack extends MusicItem {
  def id: TrackIdent

  def title: String

  def album: String

  def artist: String

  def duration: Duration

  def toTrack = SimpleTrack(id, title, album, artist, duration)
}

object BaseTrack {
  implicit val json = Format[BaseTrack](
    _.validate[SimpleTrack],
    json => Json.toJson(json.toTrack)
  )
}
