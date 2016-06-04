package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.play.Writeables
import play.api.libs.json.{Format, Json, Writes}

case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit def json(implicit t: Format[TrackMeta]) = Json.format[SavedPlaylist]

  implicit def http(implicit w: Writes[SavedPlaylist]) = Writeables.fromJson[SavedPlaylist]

  implicit def httpSeq(implicit w: Writes[SavedPlaylist]) = Writeables.fromJson[Seq[SavedPlaylist]]
}
