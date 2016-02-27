package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat

import com.malliina.musicpimp.audio.TrackMeta
import org.joda.time.DateTime
import play.api.libs.json.Json

case class RecentEntry(track: TrackMeta, when: DateTime) {
  val whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(when.toDate)
}

object RecentEntry {
  implicit val json = Json.format[RecentEntry]
}
