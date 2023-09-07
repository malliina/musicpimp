package com.malliina.musicpimp.models

import play.api.libs.json.{Json, OFormat}

case class PlaylistSnapshot(index: Int, tracks: Seq[String])

object PlaylistSnapshot {
  implicit val json: OFormat[PlaylistSnapshot] = Json.format[PlaylistSnapshot]
}
