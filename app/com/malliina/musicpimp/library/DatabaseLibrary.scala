package com.malliina.musicpimp.library

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.PimpDb
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
  * @author mle
  */
class DatabaseLibrary(db: PimpDb) extends MusicLibrary {

  def rootFolder: Future[MusicFolder] = {
    folder(Library.ROOT_ID).map(_.getOrElse(MusicFolder.empty))
  }

  def folder(id: String): Future[Option[MusicFolder]] = {
    for {
      parent <- db.folderOnly(id)
      content <- db.folder(id)
    } yield {
      parent.map { folder =>
        val (tracks, folders) = content
        MusicFolder(folder, folders, tracks)
      }
    }
  }

  def tracksIn(id: String): Future[Option[Seq[TrackMeta]]] = {
    folder(id).flatMap(maybeFolder => {
      maybeFolder
        .map(folder => Future.traverse(folder.folders)(sub => tracksInOrEmpty(sub.id)).map(subs => folder.tracks ++ subs.flatten)).map(_.map(Option.apply))
        .getOrElse(Future.successful(None))
    })
  }

  private def tracksInOrEmpty(id: String) = tracksIn(id).map(_.getOrElse(Nil))
}
