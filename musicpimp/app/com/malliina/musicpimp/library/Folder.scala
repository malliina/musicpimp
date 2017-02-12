package com.malliina.musicpimp.library

import java.nio.file.Path

/**
 * The supplied paths should be relative.
 *
 * @param dirs folders
 * @param files tracks
 */
case class Folder(dirs: Seq[Path], files: Seq[Path]) {
  def ++(other: Folder) = Folder(
    dirs = (dirs ++ other.dirs).sortBy(_.getFileName.toString.toLowerCase),
    files = (files ++ other.files).sortBy(_.getFileName.toString.toLowerCase)
  )
}

object Folder {
  val empty = Folder(Nil, Nil)
}