package com.malliina.musicpimp.models

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifiable

object CloudID extends IDCompanion[CloudID] {
  val empty = CloudID("")
}
