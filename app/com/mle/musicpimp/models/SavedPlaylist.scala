package com.mle.musicpimp.models

import com.mle.musicpimp.audio.TrackMeta
import com.mle.play.Writeables
import play.api.libs.json.Json

/**
  * @author mle
  */
case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit val json = Json.format[SavedPlaylist]
  implicit val http = Writeables.fromJson[SavedPlaylist]
  implicit val httpSeq = Writeables.fromJson[Seq[SavedPlaylist]]
}
