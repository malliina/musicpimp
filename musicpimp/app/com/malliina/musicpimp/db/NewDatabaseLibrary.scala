package com.malliina.musicpimp.db

import java.nio.file.Path

import com.malliina.musicpimp.audio.{PimpEnc, TrackMeta}
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}
import com.malliina.values.UnixPath

import scala.concurrent.{ExecutionContext, Future}

object NewDatabaseLibrary {
  def apply(db: PimpMySQL, local: Library): NewDatabaseLibrary = new NewDatabaseLibrary(db, local)
}

class NewDatabaseLibrary(val db: PimpMySQL, local: Library) extends MusicLibrary {
  import db._
  implicit val ec: ExecutionContext = db.ec

  val foldersFor = quote { (id: FolderID, path: UnixPath) =>
    foldersTable.filter(f => f.id == id || f.path == path)
  }
  val foldersQuery = quote { (id: FolderID, path: UnixPath) =>
    (for {
      f <- foldersTable.filter(_.id != lift(Library.RootId))
      parent <- foldersFor(id, path) if f.parent == parent.id
    } yield f).sortBy(_.title)
  }
  val tracksQuery = quote { (id: FolderID, path: UnixPath) =>
    for {
      t <- tracksTable
      f <- foldersFor(id, path) if t.folder == f.id
    } yield t
  }
  def rootFolder: Future[MusicFolder] =
    folder(Library.RootId).map(_.getOrElse(MusicFolder.empty))

  def folder(id: FolderID): Future[Option[MusicFolder]] = performAsync(s"Load folder $id") {
    val path = pathFromId(id)
    for {
      parent <- runIO(foldersFor(lift(id), lift(path))).map(_.headOption)
      ts <- runIO(tracksQuery(lift(id), lift(path)))
      fs <- runIO(foldersQuery(lift(id), lift(path)))
    } yield parent.map { folder =>
      MusicFolder(folder, fs, ts)
    }
  }

  def tracksIn(id: FolderID): Future[Option[Seq[TrackMeta]]] = {
    folder(id) flatMap { maybeFolder =>
      maybeFolder.map { folder =>
        Future
          .traverse(folder.folders)(sub => tracksInOrEmpty(sub.id))
          .map(subs => folder.tracks ++ subs.flatten)
      }.map(_.map(Option.apply))
        .getOrElse(Future.successful(None))
    }
  }

  private def tracksInOrEmpty(id: FolderID) = tracksIn(id).map(_.getOrElse(Nil))

  override def track(id: TrackID): Future[Option[TrackMeta]] =
    tracksFor(Seq(id)).map(_.headOption)

  override def tracks(ids: Seq[TrackID]): Future[Seq[LocalTrack]] =
    tracksFor(ids).map(_.map(local.toLocal))

  override def meta(id: TrackID): Future[Option[LocalTrack]] =
    track(id).map(_.map(local.toLocal))

  def tracksFor(ids: Seq[TrackID]): Future[Seq[DataTrack]] =
    performAsync("Load tracks for IDs") {
      IO.traverse(ids) { id =>
          runIO(quote(tracksTable.filter(t => t.id == lift(id) || t.path == lift(pathFromId(id)))))
        }
        .map { lists =>
          lists.flatten
        }
    }

  override def findFile(id: TrackID): Future[Option[Path]] = {
    track(id).map { maybeTrack =>
      maybeTrack
        .flatMap(t => local.findAbsoluteNew(t.path))
        .orElse(local.findAbsoluteLegacy(id))
    }
  }

  def insertFolders(fs: Seq[DataFolder]): Future[Long] =
    performAsync(s"Insert ${fs.length} folders") {
      runIO(liftQuery(fs).foreach(f => foldersTable.insert(f))).map(_.sum)
    }

  def insertTracks(ts: Seq[DataTrack]): Future[Long] = performAsync(s"Insert ${ts.length} tracks") {
    runIO(liftQuery(ts).foreach(t => tracksTable.insert(t))).map(_.sum)
  }

  private def pathFromId(id: Identifier): UnixPath = UnixPath.fromRaw(PimpEnc.decodeId(id))
}
