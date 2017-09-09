package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class FullPlaylistMeta(playlist: FullSavedPlaylist)

object FullPlaylistMeta {
  implicit val json = Json.format[FullPlaylistMeta]
}

case class PlaylistMeta(playlist: SavedPlaylist)
