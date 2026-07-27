package com.malliina.musicpimp.library

import cats.Functor
import cats.implicits.toFunctorOps
import com.malliina.musicpimp.models.*
import com.malliina.values.Username

trait PlaylistService[F[_]: Functor]:
  /** @return
    *   the saved playlists of user
    */
  protected def playlists(user: Username): F[Seq[SavedPlaylist]]

  /** @param id
    *   playlist id
    * @return
    *   the playlist with ID `id`, or None if no such playlist exists
    */
  protected def playlist(id: PlaylistID, user: Username): F[Option[SavedPlaylist]]

  /** @param playlist
    *   playlist submission
    * @return
    *   a Future that completes when saving is done
    */
  protected def saveOrUpdatePlaylist(
    playlist: PlaylistSubmission,
    user: Username
  ): F[PlaylistID]

  def delete(id: PlaylistID, user: Username): F[Unit]

  def playlistsMeta(user: Username): F[PlaylistsMeta] =
    playlists(user).map(PlaylistsMeta.apply)

  def playlistMeta(id: PlaylistID, user: Username): F[Option[PlaylistMeta]] =
    playlist(id, user).map(o => o.map(PlaylistMeta.apply))

  def saveOrUpdatePlaylistMeta(
    playlist: PlaylistSubmission,
    user: Username
  ): F[PlaylistSavedMeta] =
    saveOrUpdatePlaylist(playlist, user).map(PlaylistSavedMeta.apply)
