package com.malliina.musicpimp.audio

import play.api.libs.json.Json

case class Directory(folders: Seq[Folder], tracks: Seq[Track])

object Directory {
  implicit val json = Json.format[Directory]
  val empty = Directory(Nil, Nil)
}
