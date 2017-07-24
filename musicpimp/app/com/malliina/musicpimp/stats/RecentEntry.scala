package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json}

case class RecentEntry(track: TrackMeta, when: Instant) extends TopEntry {
  val whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(when))
}

object RecentEntry {
  implicit def json(implicit f: Format[TrackMeta]): Format[RecentEntry] =
    Json.format[RecentEntry]
}
