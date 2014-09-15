package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Writes}

/**
 * @author Michael
 */
trait FolderMeta {
  def id: String

  def title: String

  def path: String

  def parent: String
}

object FolderMeta {
  //  implicit val jsonFormat = Json.format[FolderMeta]
  implicit val folderWriter = new Writes[FolderMeta] {
    def writes(f: FolderMeta): JsValue = obj(
      ID -> f.id,
      TITLE -> f.title,
      PATH -> f.path,
      PARENT -> f.parent
    )
  }
}