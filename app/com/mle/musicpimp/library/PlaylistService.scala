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
    * May fail with an UnauthorizedException.
    *
    * @param playlist playlist submission
    * @param user user
    * @return a Future that completes with the playlist ID when the save operation has completed successfully, or fails
    */
  def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[PlaylistID]

  /**
    *
    * @param id playlist to delete
    * @param user user performing deletion
    * @return a Future that completes when deletion completes
    */
  def delete(id: PlaylistID, user: User): Future[Unit]
}
