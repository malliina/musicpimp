package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class PlaylistSavedMeta(id: PlaylistID)

object PlaylistSavedMeta {
  implicit val format = Json.format[PlaylistSavedMeta]
}
