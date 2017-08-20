package com.malliina.musicpimp.models

import com.malliina.play.PlayString
import slick.jdbc.H2Profile.api.{MappedColumnType, longColumnType, stringColumnType}
import play.api.data.format.Formats.stringFormat
object TrackIDs extends PlayString[TrackID] {
  implicit val trackData = MappedColumnType.base[TrackID, String](raw, apply)

  override def apply(raw: String) = TrackID(raw)

  override def raw(t: TrackID) = t.id
}
