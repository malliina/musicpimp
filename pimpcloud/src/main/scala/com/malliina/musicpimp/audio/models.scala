package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.{FolderID, MusicItem}
import io.circe.Codec

case class Directory(folders: Seq[Folder], tracks: Seq[Track]) derives Codec.AsObject

object Directory:
  val empty = Directory(Nil, Nil)

case class Folder(id: FolderID, title: String) extends MusicItem derives Codec.AsObject
