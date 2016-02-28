package com.malliina.musicpimp.models

case class PlaylistID(id: Long) {
  override def toString = s"$id"
}

object PlaylistID extends SimpleCompanion[Long, PlaylistID] {
  override def raw(t: PlaylistID): Long = t.id
}
