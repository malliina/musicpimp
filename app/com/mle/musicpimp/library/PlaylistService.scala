package com.mle.musicpimp.library

import com.mle.models.SavedPlaylist

import scala.concurrent.Future

/**
 * @author mle
 */
class PlaylistService {
  def playlist(id: String): Future[SavedPlaylist] = Future.failed(new NoSuchElementException)

  def playlists: Future[Seq[SavedPlaylist]] = Future.successful(Nil)

  def add(playlist: SavedPlaylist): Future[Unit] = Future.successful(())

  def remove(id: String): Future[Unit] = Future.successful(())
}
