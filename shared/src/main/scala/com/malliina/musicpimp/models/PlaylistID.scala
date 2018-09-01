package com.malliina.musicpimp.models

import com.malliina.values.JsonCompanion
import play.api.mvc.PathBindable

case class PlaylistID(id: Long) {
  override def toString = s"$id"
}

object PlaylistID extends JsonCompanion[Long, PlaylistID] {
  override def write(t: PlaylistID): Long = t.id

  implicit val bindable: PathBindable[PlaylistID] = PathBindable.bindableLong.transform(apply, write)
}
