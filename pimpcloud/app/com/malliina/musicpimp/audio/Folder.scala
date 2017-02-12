package com.malliina.musicpimp.audio

import com.malliina.pimpcloud.models.FolderID
import play.api.libs.json.Json

case class Folder(id: FolderID, title: String) extends MusicItem

object Folder {
  implicit val json = Json.format[Folder]
}
