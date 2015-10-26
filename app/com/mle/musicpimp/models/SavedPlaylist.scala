package com.mle.musicpimp.models

import com.mle.musicpimp.audio.TrackMeta
import com.mle.play.Writeables
import play.api.libs.json.Json

/**
 * @author mle
 */
case class SavedPlaylist(id: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit val json = Json.writes[SavedPlaylist]
  // it appears this strategy is not profitable; it appears easier to have JSON Writes-specialized Controllers
  implicit val http = Writeables.fromJson[SavedPlaylist]
  implicit val httpSeq = Writeables.fromJson[Seq[SavedPlaylist]]
}
