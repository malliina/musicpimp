package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.play.Writeables
import play.api.http.Writeable
import play.api.libs.json.{Format, Json, Writes}

case class SavedPlaylist(id: PlaylistID, name: String, tracks: Seq[TrackMeta])

object SavedPlaylist {
  implicit def json(implicit t: Format[TrackMeta]): Format[SavedPlaylist] =
    Json.format[SavedPlaylist]

  implicit def html(implicit w: Writes[SavedPlaylist]): Writeable[SavedPlaylist] =
    Writeables.fromJson[SavedPlaylist]

  implicit def htmlSeq(implicit w: Writes[SavedPlaylist]): Writeable[Seq[SavedPlaylist]] =
    Writeables.fromJson[Seq[SavedPlaylist]]
}
