package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.FolderMeta
import com.malliina.musicpimp.library.Library

/**
 * @author Michael
 */
case class DataFolder(id: String, title: String, path: String, parent: String) extends FolderMeta

object DataFolder {
  val root = DataFolder(Library.ROOT_ID, "", "", Library.ROOT_ID)

  def fromPath(p: Path) = DataFolder(Library.encode(p), p.getFileName.toString, p.toString, Library.encode(Option(p.getParent).getOrElse(Library.emptyPath)))

  def fromId(id: String) = fromPath(Library relativePath id)
}
