package com.mle.musicpimp.library

import java.nio.file.{Path, Paths}

import com.mle.musicpimp.audio.FolderMeta
import com.mle.musicpimp.db.DataFolder
import com.mle.musicpimp.json.JsonStrings._
import models.MusicItemInfo
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Writes}

/**
 *
 * @author mle
 */
class FolderInfo(id: String, title: String, val folderPath: Path)
  extends MusicItemInfo(title, id, dir = true) with FolderMeta {

  override val path: String = folderPath.toString

  override val parent: String = (Option(folderPath.getParent) getOrElse Library.emptyPath).toString
}

object FolderInfo {
  def fromData(f: DataFolder) = new FolderInfo(f.id, f.title, Paths get f.path)

  def fromPath(path: Path) =
    new FolderInfo(Library.encode(path), Option(path.getFileName).map(_.toString).getOrElse(""), path)

  implicit val folderWriter = new Writes[FolderInfo] {
    def writes(o: FolderInfo): JsValue = obj(
      ID -> o.id,
      TITLE -> o.title,
      PATH -> o.path
    )
  }
}