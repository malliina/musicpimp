package com.malliina.musicpimp.models

import play.api.libs.json.{Json, OFormat}

case class FullPlaylistMeta(playlist: FullSavedPlaylist)

object FullPlaylistMeta {
  implicit val json: OFormat[FullPlaylistMeta] = Json.format[FullPlaylistMeta]
}

case class PlaylistMeta(playlist: SavedPlaylist)
