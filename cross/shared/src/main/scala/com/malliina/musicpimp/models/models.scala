package com.malliina.musicpimp.models

import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.PlaybackStrings.{Add, AddItemsKey, Play, PlayItemsKey}
import com.malliina.values.IntValidator
import play.api.libs.json.Json

/**
  * @param id the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends AnyVal with Identifier
object CloudID extends IdentCompanion[CloudID] {
  val empty = CloudID("")
}

case class TrackID(id: String) extends AnyVal with Identifier
object TrackID extends IdentCompanion[TrackID]

case class FolderID(id: String) extends AnyVal with Identifier
object FolderID extends IdentCompanion[FolderID]

trait MusicItem {
  def id: Identifier

  def title: String
}

case class Volume private (volume: Int)

object Volume extends IntValidator[Volume] {
  override val Min = 0
  override val Max = 100

  override protected def build(t: Int) = apply(t)

  override def strip(elem: Volume) = elem.volume
}

trait TrackLike {
  def track: TrackID
}

case class PlayTrack(track: TrackID) extends TrackLike

object PlayTrack {
  val json = Json.format[PlayTrack]
  implicit val cmd = CrossFormats.cmd(Play, json)
}

case class AddTrack(track: TrackID) extends TrackLike

object AddTrack {
  val json = Json.format[AddTrack]
  implicit val cmd = CrossFormats.cmd(Add, json)
}

case class PlayItems(tracks: Seq[TrackID], folders: Seq[FolderID])

object PlayItems {
  implicit val cmd = CrossFormats.cmd(PlayItemsKey, Json.format[PlayItems])

  def folder(id: FolderID) = PlayItems(Nil, Seq(id))
}

case class AddItems(tracks: Seq[TrackID], folders: Seq[FolderID])

object AddItems {
  implicit val cmd = CrossFormats.cmd(AddItemsKey, Json.format[AddItems])

  def folder(id: FolderID) = AddItems(Nil, Seq(id))
}
