package com.mle.musicpimp.db

import java.nio.file.Path

import com.mle.musicpimp.audio.FolderMeta
import com.mle.musicpimp.library.Library

/**
 * @author Michael
 */
case class DataFolder(id: String, title: String, path: String, parent: String) extends FolderMeta

object DataFolder {
  //  def fromValues(i:String,ti:String)
  val root = DataFolder("", "", "", "")

  def fromPath(p: Path) = DataFolder(Library.encode(p), p.getFileName.toString, p.toString, Library.encode(Option(p.getParent).getOrElse(Library.emptyPath)))

  def fromId(id: String) = fromPath(Library relativePath id)
}