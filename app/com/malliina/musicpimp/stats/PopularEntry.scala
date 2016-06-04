package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json}

case class PopularEntry(track: TrackMeta, playbackCount: Int)

object PopularEntry {
  implicit def json(implicit f: Format[TrackMeta]) = Json.format[PopularEntry]
}
