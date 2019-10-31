package com.malliina.musicpimp.library

import java.nio.file.Path

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.db.PimpDb
import com.malliina.musicpimp.models.{FolderID, TrackID}

import scala.concurrent.{ExecutionContext, Future}

class DatabaseLibrary(db: PimpDb, library: Library) extends MusicLibrary {
  implicit val ec: ExecutionContext = db.ec

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
        .map(
          folder =>
            Future
              .traverse(folder.folders)(sub => tracksInOrEmpty(sub.id))
              .map(subs => folder.tracks ++ subs.flatten)
        )
        .map(_.map(Option.apply))
        .getOrElse(Future.successful(None))
    }
  }

  override def track(id: TrackID): Future[Option[TrackMeta]] =
    db.trackFor(id)

  override def meta(id: TrackID): Future[Option[LocalTrack]] =
    track(id).map(_.map(library.toLocal))

  override def tracks(ids: Seq[TrackID]): Future[Seq[LocalTrack]] =
    db.tracksFor(ids).map(_.map(library.toLocal))

  def findFile(id: TrackID): Future[Option[Path]] = {
    track(id).map { maybeTrack =>
      maybeTrack
        .flatMap(t => library.findAbsoluteNew(t.path))
        .orElse(library.findAbsoluteLegacy(id))
    }
  }

  private def tracksInOrEmpty(id: FolderID) = tracksIn(id).map(_.getOrElse(Nil))
}
