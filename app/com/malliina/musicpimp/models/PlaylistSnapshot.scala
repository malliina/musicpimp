package com.malliina.musicpimp.models

import play.api.libs.json.Json

/**
  * @author mle
  */
case class PlaylistSnapshot(index: Int, tracks: Seq[String])

object PlaylistSnapshot {
  implicit val json = Json.format[PlaylistSnapshot]
}
