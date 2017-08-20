package com.malliina.musicpimp.models

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifier {
  def toId: CloudName = CloudName(id)
}

object CloudID extends IDCompanion[CloudID] {
  val empty = CloudID("")

  def forId(id: CloudName): CloudID = apply(id.id)
}
