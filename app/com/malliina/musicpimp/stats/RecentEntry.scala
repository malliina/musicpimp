package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import org.joda.time.DateTime
import play.api.libs.json.Json

case class RecentEntry(track: TrackMeta, when: DateTime)

object RecentEntry {
  implicit val json = Json.format[RecentEntry]
}
