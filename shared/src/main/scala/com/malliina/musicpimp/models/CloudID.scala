package com.malliina.musicpimp.models

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifiable {
  def toId: CloudId = CloudId(id)
}

object CloudID extends IDCompanion[CloudID] {
  val empty = CloudID("")

  def forId(id: CloudId): CloudID = apply(id.id)
}
