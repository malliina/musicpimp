package com.malliina.musicpimp.models

import com.malliina.values.IntValidator

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

case class Volume private(volume: Int)

object Volume extends IntValidator[Volume] {
  override val Min = 0
  override val Max = 100

  override protected def build(t: Int) = apply(t)

  override def strip(elem: Volume) = elem.volume
}
