package com.mle.musicpimp.library

/**
 *
 * @author mle
 */
trait MusicLibrary {
  /**
   * @return the contents of the root library folder
   */
  def rootFolder: MusicFolder

  /**
   * @param id folder id
   * @return the contents of the library folder `id`, or None if no such folder exists
   */
  def folder(id: String): Option[MusicFolder]
}
