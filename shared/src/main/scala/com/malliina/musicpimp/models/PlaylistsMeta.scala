package com.malliina.musicpimp.models

import play.api.libs.json.{Json, OFormat}

case class FullSavedPlaylistsMeta(playlists: Seq[FullSavedPlaylist])

object FullSavedPlaylistsMeta {
  implicit val json: OFormat[FullSavedPlaylistsMeta] = Json.format[FullSavedPlaylistsMeta]
}

case class PlaylistsMeta(playlists: Seq[SavedPlaylist])
