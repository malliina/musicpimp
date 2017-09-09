package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{FullTrack, TrackJson, TrackMeta}
import play.api.libs.json.{Format, Json, Writes}

trait RecentLike extends TopEntry {
  def timestamp: Instant

  def whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(timestamp))

  def whenMillis = timestamp.toEpochMilli
}

case class FullRecentEntry(track: FullTrack, timestamp: Instant) extends RecentLike

object FullRecentEntry {
  val When = "when"
  val WhenFormatted = "whenFormatted"

  implicit val json: Format[FullRecentEntry] = {
    val base = Json.format[FullRecentEntry]
    val writer = Writes[FullRecentEntry] { r =>
      val extras = Json.obj(When -> r.whenMillis, WhenFormatted -> r.whenFormatted)
      base.writes(r) ++ extras
    }
    Format(base, writer)
  }
}

case class RecentEntry(track: TrackMeta, timestamp: Instant) extends RecentLike {
  def toFull(host: FullUrl) = FullRecentEntry(TrackJson.toFull(track, host), timestamp)
}
