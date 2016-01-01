package com.malliina.musicpimp.models

import play.api.libs.json.Json

/**
  * @author mle
  */
case class PlaylistMeta(playlist: SavedPlaylist)

object PlaylistMeta {
  implicit val format = Json.format[PlaylistMeta]
}
