package com.malliina.musicpimp.models

import play.api.libs.json.{Json, OFormat}

case class PlaylistSavedMeta(id: PlaylistID)

object PlaylistSavedMeta {
  implicit val format: OFormat[PlaylistSavedMeta] = Json.format[PlaylistSavedMeta]
}
