package com.malliina.musicpimp.models

import play.api.libs.json.{Format, Json, Reads}

case class SimpleRange(description: String) extends RangeLike

trait RangeLike {
  def description: String
}

object RangeLike {
  implicit val simple = Json.format[SimpleRange]
  implicit val json: Format[RangeLike] = Format[RangeLike](
    Reads[RangeLike](_.validate[SimpleRange]),
    r => simple.writes(SimpleRange(r.description))
  )
}

case class TrackID(id: String) extends Identifier

object TrackID extends IdentCompanion[TrackID]

case class FolderID(id: String) extends Identifier

object FolderID extends IdentCompanion[FolderID]

trait MusicItem {
  def id: Identifier

  def title: String
}
