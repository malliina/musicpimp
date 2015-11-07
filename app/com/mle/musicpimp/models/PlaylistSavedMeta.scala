package com.mle.musicpimp.models

import play.api.libs.json.Json

/**
  * @author mle
  */
case class PlaylistSavedMeta(id: PlaylistID)

object PlaylistSavedMeta {
  implicit val format = Json.format[PlaylistSavedMeta]
}
