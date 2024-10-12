package com.malliina.musicpimp.library

import com.malliina.musicpimp.models.*
import com.malliina.values.Username

import scala.concurrent.{ExecutionContext, Future}

trait PlaylistService:
  implicit def ec: ExecutionContext

  /** @return
    *   the saved playlists of user
    */
  protected def playlists(user: Username): Future[Seq[SavedPlaylist]]

  /** @param id
    *   playlist id
    * @return
    *   the playlist with ID `id`, or None if no such playlist exists
    */
  protected def playlist(id: PlaylistID, user: Username): Future[Option[SavedPlaylist]]

  /** @param playlist
    *   playlist submission
    * @return
    *   a Future that completes when saving is done
    */
  protected def saveOrUpdatePlaylist(
    playlist: PlaylistSubmission,
    user: Username
  ): Future[PlaylistID]

  def delete(id: PlaylistID, user: Username): Future[Unit]

  def playlistsMeta(user: Username): Future[PlaylistsMeta] =
    playlists(user).map(PlaylistsMeta.apply)

  def playlistMeta(id: PlaylistID, user: Username): Future[Option[PlaylistMeta]] =
    playlist(id, user).map(o => o.map(PlaylistMeta.apply))

  def saveOrUpdatePlaylistMeta(
    playlist: PlaylistSubmission,
    user: Username
  ): Future[PlaylistSavedMeta] =
    saveOrUpdatePlaylist(playlist, user).map(PlaylistSavedMeta.apply)
