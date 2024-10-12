package com.malliina.musicpimp.models

import com.malliina.json.SharedPlayFormats
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.PlaybackStrings.{Add, AddItemsKey, Play, PlayItemsKey}
import com.malliina.values.IntValidator
import io.circe.generic.semiauto.deriveCodec
import io.circe.{Codec, Decoder, Encoder}

/** @param id
  *   the cloud ID of a connected MusicPimp server
  */
case class CloudID(id: String) extends AnyVal with Identifier
object CloudID extends IdentCompanion[CloudID]:
  val empty = CloudID("")

case class TrackID(id: String) extends AnyVal with Identifier
object TrackID extends IdentCompanion[TrackID]

case class FolderID(id: String) extends AnyVal with Identifier
object FolderID extends IdentCompanion[FolderID]

trait MusicItem:
  def id: Identifier

  def title: String

case class Volume(volume: Int)

object Volume extends IntValidator[Volume]:
  override val Min = 0
  override val Max = 100

  override protected def build(t: Int): Volume = apply(t)

  override def strip(elem: Volume) = elem.volume

  given Codec[Volume] = Codec.from(
    Decoder.decodeInt.map(build),
    Encoder.encodeInt.contramap(strip)
  )

trait TrackLike:
  def track: TrackID

case class PlayTrack(track: TrackID) extends TrackLike

object PlayTrack:
  val json = deriveCodec[PlayTrack]
  implicit val cmd: Codec[PlayTrack] = CrossFormats.cmd(Play, json)

case class AddTrack(track: TrackID) extends TrackLike

object AddTrack:
  val json = deriveCodec[AddTrack]
  implicit val cmd: Codec[AddTrack] = CrossFormats.cmd(Add, json)

case class PlayItems(tracks: Seq[TrackID], folders: Seq[FolderID])

object PlayItems:
  implicit val cmd: Codec[PlayItems] = CrossFormats.cmd(PlayItemsKey, deriveCodec[PlayItems])

  def folder(id: FolderID) = PlayItems(Nil, Seq(id))

case class AddItems(tracks: Seq[TrackID], folders: Seq[FolderID])

object AddItems:
  implicit val cmd: Codec[AddItems] = CrossFormats.cmd(AddItemsKey, deriveCodec[AddItems])

  def folder(id: FolderID) = AddItems(Nil, Seq(id))
