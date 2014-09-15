package models

import com.mle.musicpimp.audio.{FolderMeta, TrackMeta}

/**
 * @author Michael
 */
case class MusicColumn(folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object MusicColumn {
  val empty = MusicColumn(Nil, Nil)
}