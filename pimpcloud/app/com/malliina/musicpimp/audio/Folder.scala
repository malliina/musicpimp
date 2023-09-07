package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.{FolderID, MusicItem}
import play.api.libs.json.{Json, OFormat}

case class Folder(id: FolderID, title: String) extends MusicItem

object Folder {
  implicit val json: OFormat[Folder] = Json.format[Folder]
}
