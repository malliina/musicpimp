package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.Json

case class PimpMostPlayedEntry(track: TrackMeta, playbackCount: Int) extends MostPlayedEntry

object PimpMostPlayedEntry {
  implicit val json = Json.format[PimpMostPlayedEntry]
}
