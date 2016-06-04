package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.FolderMeta
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.PimpPath

case class DataFolder(id: String,
                      title: String,
                      path: PimpPath,
                      parent: String) extends FolderMeta

object DataFolder {
  val root = DataFolder(Library.RootId, "", PimpPath.Empty, Library.RootId)

  def fromPath(p: Path) =
    DataFolder(
      Library.encode(p),
      p.getFileName.toString,
      PimpPath(p),
      Library.encode(Option(p.getParent).getOrElse(Library.EmptyPath)))

  def fromId(id: String) =
    fromPath(Library relativePath id)
}
