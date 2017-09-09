package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.{FullTrack, TrackMeta}
import com.malliina.play.Writeables
import play.api.http.Writeable
import play.api.libs.json.Json

case class FullSavedPlaylist(id: PlaylistID, name: String, tracks: Seq[FullTrack])

object FullSavedPlaylist {
  implicit val json = Json.format[FullSavedPlaylist]

  implicit val html: Writeable[FullSavedPlaylist] = Writeables.fromJson[FullSavedPlaylist]

  implicit val htmlSeq: Writeable[Seq[FullSavedPlaylist]] = Writeables.fromJson[Seq[FullSavedPlaylist]]
}

case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])
