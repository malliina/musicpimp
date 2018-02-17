package com.malliina.musicpimp.html

import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}
import com.malliina.musicpimp.html.PimpHtml.dataIdAttr
import com.malliina.musicpimp.js.FrontStrings._
import com.malliina.musicpimp.library.MusicFolder
import com.malliina.musicpimp.models.{FolderID, TrackID}
import controllers.musicpimp.routes

import scalatags.Text.all._

object LibraryHtml extends PimpBootstrap {

  import tags._

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
          a(href := routes.SettingsController.settings())("Music Folders"), ".")
      } else {
        empty
      }
    )
  }

  def musicItemDiv = divClass("music-item col-xs-12 col-sm-6 col-md-4 col-lg-3")

  def renderLibraryFolder(folder: FolderMeta): Modifier = Seq[Modifier](
    folderActions(folder.id),
    " ",
    a(href := routes.LibraryController.library(folder.id), `class` := s"$Lead folder-link")(folder.title)
  )

  def folderActions(folder: FolderID) =
    musicItemActions(FolderClass, folder.id, Option("folder-buttons"), aria.label := "folder action")()

  def titledTrackActions(track: TrackMeta) =
    trackActions(track.id)(
      dataButton(s"${btn.light} ${btn.block} $TrackClass $PlayClass track-title", track.id.id)(track.title)
    )

  def trackActions(track: TrackID, extraClass: Option[String] = Option("track-buttons"))(inner: Modifier*) =
    musicItemActions(TrackClass, track.id, extraClass)(inner)

  def musicItemActions(itemClazz: String, itemId: String, extraClass: Option[String], groupAttrs: Modifier*)(inner: Modifier*) = {
    val extra = extraClass.map(c => s" $c").getOrElse("")
    divClass(s"${btn.group}$extra", role := Group, groupAttrs)(
      iconicButton(s"${btn.primary} $itemClazz $PlayClass", "play-circle", itemId),
      iconicButton(s"${btn.primary} $itemClazz $AddClass", "plus", itemId),
      inner
    )
  }

  def iconicButton(clazz: String, iconName: String, buttonId: String) =
    dataButton(clazz, buttonId)(iconic(iconName))

  def dataButton(clazz: String, buttonId: String) =
    button(`type` := Button, `class` := clazz, dataIdAttr := buttonId)
}
