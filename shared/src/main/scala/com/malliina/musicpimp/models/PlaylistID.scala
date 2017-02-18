package com.malliina.musicpimp.models

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.longFormat

case class PlaylistID(id: Long) {
  override def toString = s"$id"
}

object PlaylistID extends SimpleCompanion[Long, PlaylistID] {
  override def raw(t: PlaylistID): Long = t.id
}
