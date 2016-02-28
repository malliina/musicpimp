package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.Json

case class PopularEntry(track: TrackMeta, playbackCount: Int)

object PopularEntry {
  implicit val json = Json.format[PopularEntry]
}
