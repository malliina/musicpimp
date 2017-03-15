package com.malliina.musicpimp.tags

import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}
import com.malliina.musicpimp.library.MusicFolder
import com.malliina.musicpimp.models.{FolderID, TrackID}
import com.malliina.musicpimp.tags.PimpHtml.dataIdAttr
import com.malliina.play.tags.All._
import controllers.musicpimp.routes

import scalatags.Text.all._

object LibraryHtml {
  def libraryContent(items: MusicFolder) = {
    val relativePath = items.folder.path
    Seq(
      headerDiv(
        h1("Library ", small(relativePath.path))
      ),
      row(
        items.folders map { folder =>
          musicItemDiv(
            renderLibraryFolder(folder)
          )
        },
        items.tracks map { track =>
          musicItemDiv(
            titledTrackActions(track)
          )
        }
      ),
      if (items.isEmpty && relativePath.path.isEmpty) {
        leadPara("The library is empty. To get started, add music folders under ",
          aHref(routes.SettingsController.settings())("Music Folders"), ".")
      } else {
        empty
      }
    )
  }

  def musicItemDiv = divClass("music-item col-xs-12 col-sm-6 col-md-4 col-lg-3")

  def renderLibraryFolder(folder: FolderMeta): Modifier = Seq[Modifier](
    folderActions(folder.id),
    " ",
    aHref(routes.LibraryController.library(folder.id), `class` := s"$Lead folder-link")(folder.title)
  )

  def folderActions(folder: FolderID) =
    musicItemActions("folder", folder.id, Option("folder-buttons"), ariaLabel := "folder action")()

  def titledTrackActions(track: TrackMeta) =
    trackActions(track.id)(
      dataButton(s"$BtnDefault $BtnBlock track play track-title", track.id.id)(track.title)
    )

  def trackActions(track: TrackID, extraClass: Option[String] = Option("track-buttons"))(inner: Modifier*) =
    musicItemActions("track", track.id, extraClass)(inner)

  def musicItemActions(itemClazz: String, itemId: String, extraClass: Option[String], groupAttrs: Modifier*)(inner: Modifier*) = {
    val extra = extraClass.map(c => s" $c").getOrElse("")
    divClass(s"$BtnGroup$extra", role := Group, groupAttrs)(
      glyphButton(s"$BtnPrimary $itemClazz play", "play", itemId),
      glyphButton(s"$BtnDefault $itemClazz add", "plus", itemId),
      inner
    )
  }

  def glyphButton(clazz: String, glyph: String, buttonId: String) =
    dataButton(clazz, buttonId)(glyphIcon(glyph))

  def dataButton(clazz: String, buttonId: String) =
    button(`type` := Button, `class` := clazz, dataIdAttr := buttonId)
}
