package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class PlaylistsMeta(playlists: Seq[SavedPlaylist])

object PlaylistsMeta {
  implicit val format = Json.format[PlaylistsMeta]
}
