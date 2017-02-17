package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.{FolderMeta, PimpEnc}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, PimpPath}

case class DataFolder(id: FolderID,
                      title: String,
                      path: PimpPath,
                      parent: FolderID) extends FolderMeta

object DataFolder {
  val root = DataFolder(Library.RootId, "", PimpPath.Empty, Library.RootId)

  def fromPath(p: Path) =
    DataFolder(
      PimpEnc.encodeFolder(p),
      p.getFileName.toString,
      PimpPath(p),
      PimpEnc.encodeFolder(Option(p.getParent).getOrElse(Library.EmptyPath)))
}
