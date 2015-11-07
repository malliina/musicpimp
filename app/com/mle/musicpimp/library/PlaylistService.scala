package com.mle.musicpimp.library

import com.mle.musicpimp.models._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * @author mle
  */
trait PlaylistService {
  /**
    * @return the saved playlists of user
    */
  protected def playlists(user: User): Future[Seq[SavedPlaylist]]

  /**
    * @param id playlist id
    * @return the playlist with ID `id`, or None if no such playlist exists
    */
  protected def playlist(id: PlaylistID, user: User): Future[Option[SavedPlaylist]]

  /**
    * @param playlist playlist submission
    * @return a Future that completes when saving is done
    */
  def saveOrUpdatePlaylist(playlist: PlaylistSubmission, user: User): Future[Unit]

  def delete(id: PlaylistID, user: User): Future[Unit]

  def playlistsMeta(user: User): Future[PlaylistsMeta] = {
    playlists(user).map(PlaylistsMeta.apply)
  }

  def playlistMeta(id: PlaylistID, user: User): Future[Option[PlaylistMeta]] = {
    playlist(id, user).map(o => o.map(PlaylistMeta.apply))
  }
}
