package com.malliina.musicpimp.models

import com.malliina.play.PlayString

object TrackIDs extends PlayString[TrackID] {
  override def apply(raw: String) = TrackID(raw)

  override def raw(t: TrackID) = t.id
}
