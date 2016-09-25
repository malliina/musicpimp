package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.models.FolderID
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

class DatabaseLibrary(db: PimpDb) extends MusicLibrary {

  def rootFolder: Future[MusicFolder] =
    folder(Library.RootId).map(_.getOrElse(MusicFolder.empty))

  def folder(id: FolderID): Future[Option[MusicFolder]] = {
    for {
      parent <- db.folderOnly(id)
      content <- db.folder(id)
    } yield {
      parent map { folder =>
        val (tracks, folders) = content
        MusicFolder(folder, folders, tracks)
      }
    }
  }

  def tracksIn(id: FolderID): Future[Option[Seq[TrackMeta]]] = {
    folder(id) flatMap { maybeFolder =>
      maybeFolder
        .map(folder => Future.traverse(folder.folders)(sub => tracksInOrEmpty(sub.id)).map(subs => folder.tracks ++ subs.flatten)).map(_.map(Option.apply))
        .getOrElse(Future.successful(None))
    }
  }

  private def tracksInOrEmpty(id: FolderID) = tracksIn(id).map(_.getOrElse(Nil))
}
