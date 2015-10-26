package com.mle.musicpimp.library

import scala.concurrent.Future

/**
 *
 * @author mle
 */
trait MusicLibrary {
  /**
   * @return the contents of the root library folder
   */
  def rootFolder: Future[MusicFolder]

  /**
   * @param id folder id
   * @return the contents of the library folder `id`, or None if no such folder exists
   */
  def folder(id: String): Future[Option[MusicFolder]]
}
