package com.malliina.musicpimp.library

import java.nio.file.Path

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.{FolderID, TrackID}

import scala.concurrent.Future

trait MusicLibrary {
  /**
    * @return the contents of the root library folder
    */
  def rootFolder: Future[MusicFolder]

  /**
    * @param id folder id
    * @return the contents of the library folder `id`, or None if no such folder exists
    */
  def folder(id: FolderID): Future[Option[MusicFolder]]

  /**
    * @param id folder ID
    * @return all the tracks, recursively, in folder `id`
    */
  def tracksIn(id: FolderID): Future[Option[Seq[TrackMeta]]]

  def track(id: TrackID): Future[Option[TrackMeta]]

  def findFile(id: TrackID): Future[Option[Path]]

  def meta(id: TrackID): Future[Option[LocalTrack]]

  def tracks(ids: Seq[TrackID]): Future[Seq[LocalTrack]]
}
