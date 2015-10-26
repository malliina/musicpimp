package com.mle.musicpimp.db

import java.nio.file.Path

import com.mle.musicpimp.audio.FolderMeta
import com.mle.musicpimp.library.Library

/**
 * @author Michael
 */
case class DataFolder(id: String, title: String, path: String, parent: String) extends FolderMeta

object DataFolder {
  val root = DataFolder(Library.ROOT_ID, "", "", Library.ROOT_ID)

  def fromPath(p: Path) = DataFolder(Library.encode(p), p.getFileName.toString, p.toString, Library.encode(Option(p.getParent).getOrElse(Library.emptyPath)))

  def fromId(id: String) = fromPath(Library relativePath id)
}
