package com.malliina.musicpimp.stats

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{FullTrack, TrackJson, TrackMeta}
import io.circe.{Codec, Encoder, Json}
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps

trait RecentLike extends TopEntry:
  def timestamp: Instant
  def whenFormatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(timestamp))
  def whenMillis = timestamp.toEpochMilli

case class FullRecentEntry(track: FullTrack, timestamp: Instant) extends RecentLike

object FullRecentEntry:
  val When = "when"
  val WhenFormatted = "whenFormatted"

  implicit val json: Codec[FullRecentEntry] =
    val base = deriveCodec[FullRecentEntry]
    val writer = Encoder[FullRecentEntry]: r =>
      val extras = Json.obj(When -> r.whenMillis.asJson, WhenFormatted -> r.whenFormatted.asJson)
      base(r).deepMerge(extras)
    Codec.from(base, writer)

case class RecentEntry(track: TrackMeta, timestamp: Instant) extends RecentLike:
  def toFull(host: FullUrl) = FullRecentEntry(TrackJson.toFull(track, host), timestamp)
