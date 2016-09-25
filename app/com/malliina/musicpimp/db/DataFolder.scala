package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.FolderMeta
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
      Library.encodeFolder(p),
      p.getFileName.toString,
      PimpPath(p),
      Library.encodeFolder(Option(p.getParent).getOrElse(Library.EmptyPath)))

  def fromId(id: String) =
    fromPath(Library relativePath id)
}
