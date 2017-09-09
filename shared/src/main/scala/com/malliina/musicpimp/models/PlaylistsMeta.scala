package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class FullSavedPlaylistsMeta(playlists: Seq[FullSavedPlaylist])

object FullSavedPlaylistsMeta {
  implicit val json = Json.format[FullSavedPlaylistsMeta]
}

case class PlaylistsMeta(playlists: Seq[SavedPlaylist])
