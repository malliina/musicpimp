package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat

import com.malliina.musicpimp.audio.TrackMeta
import org.joda.time.DateTime
import play.api.libs.json.{Format, Json}

case class RecentEntry(track: TrackMeta, when: DateTime) extends TopEntry {
  val whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(when.toDate)
}

object RecentEntry {
  implicit def json(implicit f: Format[TrackMeta]) = Json.format[RecentEntry]
}
