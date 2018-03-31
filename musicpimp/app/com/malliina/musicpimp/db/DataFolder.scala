package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.{FolderMeta, PimpEnc}
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.FolderID
import com.malliina.values.UnixPath
import play.api.libs.json.{Json, OFormat}

case class DataFolder(id: FolderID,
                      title: String,
                      path: UnixPath,
                      parent: FolderID) extends FolderMeta

object DataFolder {
  implicit val json: OFormat[DataFolder] = Json.format[DataFolder]
  val root = DataFolder(Library.RootId, "", UnixPath.Empty, Library.RootId)

  def fromPath(p: Path) =
    DataFolder(
      PimpEnc.encodeFolder(p),
      p.getFileName.toString,
      UnixPath(p),
      PimpEnc.encodeFolder(Option(p.getParent).getOrElse(Library.EmptyPath))
    )
}
