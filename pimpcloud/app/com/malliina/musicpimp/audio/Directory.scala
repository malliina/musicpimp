package com.malliina.musicpimp.audio

import play.api.libs.json.{Json, OFormat}

case class Directory(folders: Seq[Folder], tracks: Seq[Track])

object Directory {
  implicit val json: OFormat[Directory] = Json.format[Directory]
  val empty = Directory(Nil, Nil)
}
