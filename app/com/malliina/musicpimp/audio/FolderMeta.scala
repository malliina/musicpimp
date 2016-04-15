package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.MusicItem
import com.malliina.musicpimp.json.JsonStrings._
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Writes}

trait FolderMeta extends MusicItem {
  def id: String

  def title: String

  def path: String

  def parent: String
}

object FolderMeta {
  implicit val folderWriter = new Writes[FolderMeta] {
    def writes(f: FolderMeta): JsValue = obj(
      ID -> f.id,
      TITLE -> f.title,
      PATH -> f.path
      //      PARENT -> f.parent
    )
  }
}