package com.malliina.musicpimp.library

import java.nio.file.Path

import com.malliina.musicpimp.audio.{FolderMeta, PimpEnc}
import com.malliina.musicpimp.models.FolderID
import com.malliina.values.UnixPath

class FolderInfo(val id: FolderID, val title: String, val folderPath: Path) extends FolderMeta:
  override val path = UnixPath(folderPath)
  override val parent: FolderID =
    FolderID((Option(folderPath.getParent) getOrElse Library.EmptyPath).toString)

object FolderInfo:
  def fromPath(path: Path) = new FolderInfo(
    FolderID(Library.idFor(UnixPath(path).path)),
    Option(path.getFileName).map(_.toString).getOrElse(""),
    path
  )
