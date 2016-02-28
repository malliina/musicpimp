package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.play.Writeables
import play.api.libs.json.Json

case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit val json = Json.format[SavedPlaylist]
  implicit val http = Writeables.fromJson[SavedPlaylist]
  implicit val httpSeq = Writeables.fromJson[Seq[SavedPlaylist]]
}
