package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.{FolderID, MusicItem}
import io.circe.Codec

case class Folder(id: FolderID, title: String) extends MusicItem derives Codec.AsObject
