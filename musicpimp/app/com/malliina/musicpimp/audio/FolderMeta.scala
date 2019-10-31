package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, MusicItem}
import com.malliina.play.http.FullUrls
import com.malliina.values.UnixPath
import play.api.libs.json.Json.obj
import play.api.libs.json.{Json, Writes}
import play.api.mvc.Call

case class FullFolder(
  id: FolderID,
  title: String,
  path: UnixPath,
  src: UnixPath,
  parent: FolderID,
  url: FullUrl
)

object FullFolder {
  implicit val json = Json.format[FullFolder]
}

trait FolderMeta extends MusicItem {
  def id: FolderID

  def title: String

  def path: UnixPath

  /**
    * @return the parent folder, but the root folder if this folder is the root folder
    */
  def parent: FolderID

  def toFull(host: FullUrl) =
    FullFolder(id, title, path, path, parent, FolderMeta.urlFor(host, id))
}

object FolderMeta {
  val libraryController = controllers.musicpimp.routes.LibraryController

  def urlFor(host: FullUrl, id: FolderID) = {
    val call: Call =
      if (id == Library.RootId) libraryController.rootLibrary()
      else libraryController.library(id)
    FullUrls.absolute(host, call)
  }

  def writer(host: FullUrl) = Writes[FolderMeta] { f =>
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
