package com.malliina.musicpimp.models

import play.api.libs.json.Json

case class PlaylistSnapshot(index: Int, tracks: Seq[String])

object PlaylistSnapshot {
  implicit val json = Json.format[PlaylistSnapshot]
}
