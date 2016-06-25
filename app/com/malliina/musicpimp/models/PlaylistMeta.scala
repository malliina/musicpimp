package com.malliina.musicpimp.models

import play.api.libs.json.{Format, Json}

case class PlaylistMeta(playlist: SavedPlaylist)

object PlaylistMeta {
  implicit def format(implicit f: Format[SavedPlaylist]) = Json.format[PlaylistMeta]
}
