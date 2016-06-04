package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{MusicItem, PimpPath, PimpUrl}
import play.api.libs.json.Json.obj
import play.api.libs.json.Writes

trait FolderMeta extends MusicItem {
  def id: String

  def title: String

  def path: PimpPath

  def parent: String
}

object FolderMeta {
  def writer(host: PimpUrl) = Writes[FolderMeta] { f =>
    val call = controllers.routes.LibraryController.library(f.id)
    obj(
      Id -> f.id,
      Title -> f.title,
      PathKey -> f.path,
      Url -> host.absolute(call)
    )
  }
}
