package com.malliina.musicpimp.stats

import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{FullTrack, TrackJson, TrackMeta}
import com.malliina.musicpimp.db.DataTrack
import io.circe.Codec
import play.api.libs.json.{Json, OFormat}

trait PopularLike extends TopEntry:
  def playbackCount: Int

case class FullPopularEntry(track: FullTrack, playbackCount: Int) extends PopularLike
  derives Codec.AsObject

case class PopularEntry(track: DataTrack, playbackCount: Int) extends PopularLike:
  def toFull(host: FullUrl) = FullPopularEntry(TrackJson.toFull(track, host), playbackCount)
