package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.FolderMeta
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.FolderID
import com.malliina.values.UnixPath
import io.getquill.Embedded
import play.api.libs.json.{Json, OFormat}

case class DataFolder(id: FolderID, title: String, path: UnixPath, parent: FolderID)
  extends FolderMeta
  with Embedded

object DataFolder {
  implicit val json: OFormat[DataFolder] = Json.format[DataFolder]
  val root = DataFolder(Library.RootId, "", UnixPath.Empty, Library.RootId)

  def fromPath(p: Path) =
    DataFolder(
      Library.folderId(p),
      p.getFileName.toString,
      UnixPath(p),
      Library.parent(p)
    )
}
