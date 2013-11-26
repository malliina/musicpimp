package com.mle.musicpimp.library

import models.MusicItemInfo
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonStrings._
import java.nio.file.Path
import play.api.libs.json.{JsValue, Writes}

/**
 *
 * @author mle
 */
class FolderInfo(id: String, title: String, val path: Path)
  extends MusicItemInfo(title, id, dir = true)

object FolderInfo {
  implicit val folderWriter = new Writes[FolderInfo] {
    def writes(o: FolderInfo): JsValue = obj(
      ID -> o.id,
      TITLE -> o.title,
      PATH -> o.path.toString
    )
  }
}