package com.malliina.musicpimp.db

import com.malliina.musicpimp.library.{MusicFolder, PlaylistSubmission}
import com.malliina.musicpimp.models.{FolderID, PlaylistID, SavedPlaylist}
import com.malliina.play.models.Username

import scala.concurrent.Future

trait MusicDatabase {
  def rootFolder: Future[MusicFolder]

  def folder(id: FolderID): Future[Option[MusicFolder]]

  def playlists(user: Username): Future[Seq[SavedPlaylist]]

  def playlist(id: PlaylistID): Future[Option[SavedPlaylist]]

  def saveOrUpdatePlaylist(playlist: PlaylistSubmission): Future[Unit]
}
