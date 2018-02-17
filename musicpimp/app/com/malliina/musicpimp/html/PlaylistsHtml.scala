package com.malliina.musicpimp.html

import com.malliina.musicpimp.models.SavedPlaylist
import controllers.musicpimp.routes

import scalatags.Text.all._

object PlaylistsHtml extends PimpBootstrap {

  def playlistsContent(lists: Seq[SavedPlaylist]) = Seq(
    headerRow("Playlists"),
    tableView("No saved playlists.", lists, "Name", "Tracks", "Actions") { list =>
      Seq(
        td(a(href := routes.Playlists.playlist(list.id))(list.name)),
        td(list.tracks.size),
        td("Add/Edit/Delete")
      )
    }
  )

  def playlistContent(playlist: SavedPlaylist) = Seq(
    headerRow("Playlist"),
    leadPara(playlist.name),
    tableView("This playlist is empty.", playlist.tracks, "Title", "Album", "Artist") { track =>
      Seq(td(track.title), td(track.album), td(track.artist))
    }
  )

  def tableView[T](emptyText: String, items: Seq[T], headers: String*)(cells: T => Seq[Modifier]) =
    fullRow(
      if (items.isEmpty) {
        leadPara(emptyText)
      } else {
        responsiveTable(items)(headers: _*)(cells)
      }
    )
}
