package com.malliina.musicpimp.models

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends Identifier

object CloudID extends IdentCompanion[CloudID] {
  val empty = CloudID("")
}

case class TrackID(id: String) extends Identifier

object TrackID extends IdentCompanion[TrackID]

case class FolderID(id: String) extends Identifier

object FolderID extends IdentCompanion[FolderID]

trait MusicItem {
  def id: Identifier

  def title: String
}
