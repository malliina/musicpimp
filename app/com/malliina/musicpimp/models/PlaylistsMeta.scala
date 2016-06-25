package com.malliina.musicpimp.models

import com.malliina.musicpimp.audio.TrackMeta
import play.api.libs.json.{Format, Json}

case class PlaylistsMeta(playlists: Seq[SavedPlaylist])

object PlaylistsMeta {
  implicit def format(implicit f: Format[TrackMeta]): Format[PlaylistsMeta] =
    Json.format[PlaylistsMeta]
}
