package com.malliina.musicpimp.library

import java.nio.file.Path

import com.malliina.musicpimp.models.MusicItem
import com.malliina.musicpimp.audio.FolderMeta

/**
 *
 * @author mle
 */
class FolderInfo(val id: String, val title: String, val folderPath: Path) extends MusicItem with FolderMeta {
  override val path: String = folderPath.toString
  override val parent: String = (Option(folderPath.getParent) getOrElse Library.emptyPath).toString
}

object FolderInfo {
  def fromPath(path: Path) =
    new FolderInfo(Library.encode(path), Option(path.getFileName).map(_.toString).getOrElse(""), path)
}