package com.malliina.musicpimp.models

import play.api.libs.json.Json

/**
  * @author mle
  */
case class PlaylistsMeta(playlists: Seq[SavedPlaylist])

object PlaylistsMeta {
  implicit val format = Json.format[PlaylistsMeta]
}
