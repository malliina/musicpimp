package com.malliina.musicpimp.db

import com.malliina.musicpimp.audio.FolderMeta
import com.malliina.musicpimp.db.DoobieMappings.given
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.FolderID
import com.malliina.values.UnixPath
import io.circe.Codec

import java.nio.file.Path

case class DataFolder(id: FolderID, title: String, path: UnixPath, parent: FolderID)
  extends FolderMeta derives Codec.AsObject, doobie.Read

object DataFolder:
  val root = DataFolder(Library.RootId, "", UnixPath.Empty, Library.RootId)

  def fromPath(p: Path) =
    DataFolder(
      Library.folderId(p),
      p.getFileName.toString,
      UnixPath(p),
      Library.parent(p)
    )
