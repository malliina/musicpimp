package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, MusicItem}
import com.malliina.http.FullUrl
import com.malliina.play.http.FullUrls
import com.malliina.values.UnixPath
import play.api.libs.json.Json.obj
import play.api.libs.json.Writes
import play.api.mvc.Call

trait FolderMeta extends MusicItem {
  def id: FolderID

  def title: String

  def path: UnixPath

  /**
    * @return the parent folder, but the root folder if this folder is the root folder
    */
  def parent: FolderID
}

object FolderMeta {
  def writer(host: FullUrl) = Writes[FolderMeta] { f =>
    val libraryController = controllers.musicpimp.routes.LibraryController
    val call: Call =
      if (f.id == Library.RootId) libraryController.rootLibrary()
      else libraryController.library(f.id)
    obj(
      Id -> f.id,
      Title -> f.title,
      PathKey -> UnixPath.json.writes(f.path),
      Url -> FullUrls.absolute(host, call)
    )
  }
}
