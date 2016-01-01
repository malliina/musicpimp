package com.malliina.musicpimp.db

import com.malliina.musicpimp.models.{SavedPlaylist, User}
import com.malliina.musicpimp.library.{MusicFolder, PlaylistSubmission}

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
