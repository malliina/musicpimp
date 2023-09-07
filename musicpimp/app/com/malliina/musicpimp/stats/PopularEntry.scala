package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{FullTrack, TrackJson, TrackMeta}
import play.api.libs.json.{Json, OFormat}

trait PopularLike extends TopEntry {
  def playbackCount: Int
}

case class FullPopularEntry(track: FullTrack, playbackCount: Int) extends PopularLike

object FullPopularEntry {
  implicit val json: OFormat[FullPopularEntry] = Json.format[FullPopularEntry]
}

case class PopularEntry(track: TrackMeta, playbackCount: Int) extends PopularLike {
  def toFull(host: FullUrl) = FullPopularEntry(TrackJson.toFull(track, host), playbackCount)
}
