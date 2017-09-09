package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json, Writes}

case class RecentEntry(track: TrackMeta, timestamp: Instant) extends TopEntry {
  val whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(timestamp))

  def whenMillis = timestamp.toEpochMilli
}

object RecentEntry {
  val When = "when"
  val WhenFormatted = "whenFormatted"

  implicit def json(implicit f: Format[TrackMeta]): Format[RecentEntry] = {
    val base = Json.format[RecentEntry]
    val writer = Writes[RecentEntry] { r =>
      val extras = Json.obj(When -> r.whenMillis, WhenFormatted -> r.whenFormatted)
      base.writes(r) ++ extras
    }
    Format(base, writer)
  }
}
