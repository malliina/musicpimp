package com.malliina.musicpimp.models

case class TrackID(id: String) extends TrackIdent {
  def toId = TrackIdentifier(id)
}

object TrackID extends IDCompanion[TrackID]
