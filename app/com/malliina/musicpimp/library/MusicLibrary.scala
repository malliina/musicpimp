package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.FolderID

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
}
