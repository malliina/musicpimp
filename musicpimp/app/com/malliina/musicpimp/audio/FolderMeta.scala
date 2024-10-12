package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.JsonStrings.*
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{FolderID, MusicItem}
import com.malliina.play.http.FullUrls
import com.malliina.values.UnixPath
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Encoder, Json}
import play.api.mvc.Call

case class FullFolder(
  id: FolderID,
  title: String,
  path: UnixPath,
  src: UnixPath,
  parent: FolderID,
  url: FullUrl
) derives Codec.AsObject

trait FolderMeta extends MusicItem:
  def id: FolderID

  def title: String

  def path: UnixPath

  /** @return
    *   the parent folder, but the root folder if this folder is the root folder
    */
  def parent: FolderID

  def toFull(host: FullUrl) =
    FullFolder(id, title, path, path, parent, FolderMeta.urlFor(host, id))

object FolderMeta:
  val libraryController = controllers.musicpimp.routes.LibraryController

  def urlFor(host: FullUrl, id: FolderID) =
    val call: Call =
      if id == Library.RootId then libraryController.rootLibrary
      else libraryController.library(id)
    FullUrls.absolute(host, call)

  def writer(host: FullUrl) = Encoder[FolderMeta]: f =>
    val call: Call =
      if f.id == Library.RootId then libraryController.rootLibrary
      else libraryController.library(f.id)
    Json.obj(
      Id -> f.id.asJson,
      Title -> f.title.asJson,
      PathKey -> UnixPath.json(f.path).asJson,
      Url -> FullUrls.absolute(host, call).asJson
    )
