package com.mle.musicpimp.library

import com.mle.musicpimp.models.{PlaylistID, User}

import scala.concurrent.Future

/**
 * @author mle
 */
trait PlaylistService {
  /**
   * @return the saved playlists of user
   */
  def playlists(user: User): Future[Seq[SavedPlaylist]]

  /**
   * @param id playlist id
   * @return the playlist with ID `id`, or None if no such playlist exists
   */
  def playlist(id: PlaylistID, user: User): Future[Option[SavedPlaylist]]

  /**
   * @param playlist playlist submission
   * @return a Future that completes when saving is done
   */
  def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[Unit]

  def delete(id: PlaylistID, user: User): Future[Unit]
}
