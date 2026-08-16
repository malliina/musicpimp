package com.malliina.musicpimp.audio

import io.circe.Codec

case class Directory(folders: Seq[Folder], tracks: Seq[Track]) derives Codec.AsObject

object Directory:
  val empty = Directory(Nil, Nil)
