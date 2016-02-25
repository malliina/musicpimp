package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}
import com.malliina.musicpimp.db.DataFolder
import play.api.libs.json.Json

case class MusicFolder(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object MusicFolder {
  val empty = MusicFolder(DataFolder.root, Nil, Nil)
  implicit val format = Json.writes[MusicFolder]
}
