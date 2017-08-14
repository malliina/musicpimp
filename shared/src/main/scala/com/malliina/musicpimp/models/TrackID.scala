package com.malliina.musicpimp.models

case class TrackID(id: String) extends Ident

object TrackID extends IDCompanion[TrackID]
