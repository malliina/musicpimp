package com.mle.musicpimp.library

import java.nio.file.Path

/**
 *
 * @author mle
 */
trait MusicLibrary {
  /**
   * @return the contents of the root library folder
   */
  def rootItems: Folder

  /**
   * Returns the contents at `relative`. This library may manage multiple sources,
   * each of which may have content at `relative`. In that case, the returned folder
   * containts the merged contents of all such sources.
   *
   * @param relative library path
   * @return the contents of the library at path `relative`, or None if no source has a folder at path `relative`
   */
  def items(relative: Path): Option[Folder]
}
