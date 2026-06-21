package com.malliina.musicpimp.library

import cats.data.NonEmptyList

import java.nio.file.Path
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.{FolderID, TrackID}

trait MusicLibrary[F[_]]:

  /** @return
    *   the contents of the root library folder
    */
  def rootFolder: F[MusicFolder]

  /** @param id
    *   folder id
    * @return
    *   the contents of the library folder `id`, or None if no such folder exists
    */
  def folder(id: FolderID): F[Option[MusicFolder]]

  /** @param id
    *   folder ID
    * @return
    *   all the tracks, recursively, in folder `id`
    */
  def tracksIn(id: FolderID): F[Option[List[TrackMeta]]]
  def track(id: TrackID): F[Option[TrackMeta]]
  def findFile(id: TrackID): F[Option[Path]]
  def meta(id: TrackID): F[Option[LocalTrack]]
  def tracks(ids: NonEmptyList[TrackID]): F[List[LocalTrack]]
