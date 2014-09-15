package com.mle.musicpimp.library

import java.nio.file.Path

import com.mle.musicpimp.audio.{FolderMeta, TrackMeta}
import com.mle.musicpimp.db.DataFolder
import play.api.libs.json.Json

/**
 * @author Michael
 */
case class MusicCollection(folder: FolderMeta, folders: Seq[FolderMeta], tracks: Seq[TrackMeta]) {
  val isEmpty = folders.isEmpty && tracks.isEmpty
}

object MusicCollection {
//  implicit val folderWriter = FolderMeta.folderWriter
  implicit val format = Json.writes[MusicCollection]
  //  implicit val writes = new Writes[MusicCollection] {
  //    def writes(collection: MusicCollection): JsValue = {
  //      val tracks: Seq[LocalTrack] = collection.tracks
  //      obj(
  //        FOLDER -> obj(
  //          ID -> collection.id,
  //          TITLE -> toJson(Option(collection.path.getFileName).map(_.toString).getOrElse("")),
  //          PATH -> collection.path.toString
  //        ),
  //        FOLDERS -> toJson(collection.folders),
  //        TRACKS -> toJson(tracks)
  //      )
  //    }
  //  }

  def fromFolder(id: String, path: Path, folder: Folder): MusicCollection = {
    val baseFolder = DataFolder.fromPath(path).copy(id = id)
    val dirs = folder.dirs map DataFolder.fromPath
    val songs = folder.files map Library.meta
    MusicCollection(baseFolder, dirs, songs)
  }
}