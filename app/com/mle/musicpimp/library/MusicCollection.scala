package com.mle.musicpimp.library

import java.nio.file.Path
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsValue, Writes}
import com.mle.musicpimp.audio.TrackMeta

/**
 * @author Michael
 */
case class MusicCollection(id: String, path: Path, dirs: Seq[FolderInfo], songs: Seq[LocalTrack])

object MusicCollection {
  implicit val writes = new Writes[MusicCollection] {
    def writes(o: MusicCollection): JsValue = {
      val tracks:Seq[TrackMeta] = o.songs
      obj(
        FOLDER -> obj(
          ID -> o.id,
          TITLE -> toJson(Option(o.path.getFileName).map(_.toString).getOrElse("")),
          PATH -> o.path.toString
        ),
        FOLDERS -> toJson(o.dirs),
        TRACKS -> toJson(tracks)
      )
    }
  }

  def fromFolder(id: String, path: Path, folder: Folder) = {
    val dirs = folder.dirs map toFolder
    val songs = folder.files map Library.meta
    MusicCollection(id, path, dirs, songs)
  }

  def toFolder(path: Path) = {
    new FolderInfo(Library.encode(path), Option(path.getFileName).map(_.toString).getOrElse(""), path)
  }
}