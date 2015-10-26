package com.mle.musicpimp.db

import com.mle.musicpimp.models.User
import com.mle.musicpimp.library.{MusicFolder, PlaylistSubmission, SavedPlaylist}

import scala.concurrent.Future

/**
 * @author mle
 */
trait MusicDatabase {
  def rootFolder: Future[MusicFolder]

  def folder(id: String): Future[Option[MusicFolder]]

  def playlists(user: User): Future[Seq[SavedPlaylist]]

  def playlist(id: Long): Future[Option[SavedPlaylist]]

  def saveOrUpdatePlaylist(playlist: PlaylistSubmission): Future[Unit]
}
