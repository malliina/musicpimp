package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.{FolderMeta, TrackMeta}
import com.malliina.musicpimp.db.DataFolder
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class MusicFolder(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object MusicFolder {
  val empty = MusicFolder(DataFolder.root, Nil, Nil)
  implicit val format = Json.writes[MusicFolder]

//  def fromFolder(id: String, path: Path, folder: Folder): MusicFolder = {
//    val baseFolder = DataFolder.fromPath(path).copy(id = id)
//    val dirs = folder.dirs map DataFolder.fromPath
//    val songs = folder.files map Library.meta
//    MusicFolder(baseFolder, dirs, songs)
//  }
}
