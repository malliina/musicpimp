package com.malliina.musicpimp.db

import java.nio.file.Path
import com.malliina.musicpimp.audio.{PimpEnc, TrackMeta}
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}
import com.malliina.values.UnixPath
import io.getquill.*

import scala.concurrent.{ExecutionContext, Future}

object NewDatabaseLibrary:
  def apply(db: PimpMySQL, local: Library): NewDatabaseLibrary = new NewDatabaseLibrary(db, local)

class NewDatabaseLibrary(val db: PimpMySQL, local: Library) extends MusicLibrary:
  import db.*
  implicit val ec: ExecutionContext = db.ec

  def foldersFor(id: FolderID, path: UnixPath) = quote:
    foldersTable.filter(f => f.id == lift(id) || f.path == lift(path))
  def foldersQuery(id: FolderID, path: UnixPath) = quote:
    (for
      f <- foldersTable.filter(_.id != lift(Library.RootId))
      parent <- foldersFor(id, path) if f.parent == parent.id
    yield f).sortBy(_.title)
  def tracksQuery(id: FolderID, path: UnixPath) = quote:
    for
      t <- tracksTable
      f <- foldersFor(id, path) if t.folder == f.id
    yield t
  def rootFolder: Future[MusicFolder] =
    folder(Library.RootId).map(_.getOrElse(MusicFolder.empty))

  def folder(id: FolderID): Future[Option[MusicFolder]] = performAsync(s"Load folder $id"):
    val path = pathFromId(id)
    val parent = run(foldersFor(id, path)).headOption
    val ts = run(tracksQuery(id, path))
    val fs = run(foldersQuery(id, path))
    parent.map: folder =>
      MusicFolder(folder, fs, ts)

  def tracksIn(id: FolderID): Future[Option[Seq[TrackMeta]]] =
    folder(id).flatMap: maybeFolder =>
      maybeFolder
        .map: folder =>
          Future
            .traverse(folder.folders)(sub => tracksInOrEmpty(sub.id))
            .map(subs => folder.tracks ++ subs.flatten)
        .map(_.map(Option.apply))
        .getOrElse(Future.successful(None))

  private def tracksInOrEmpty(id: FolderID) = tracksIn(id).map(_.getOrElse(Nil))

  override def track(id: TrackID): Future[Option[TrackMeta]] =
    tracksFor(Seq(id)).map(_.headOption)

  override def tracks(ids: Seq[TrackID]): Future[Seq[LocalTrack]] =
    tracksFor(ids).map(_.map(local.toLocal))

  override def meta(id: TrackID): Future[Option[LocalTrack]] =
    track(id).map(_.map(local.toLocal))

  def tracksFor(ids: Seq[TrackID]): Future[Seq[DataTrack]] =
    performAsync("Load tracks for IDs"):
      ids.flatMap: id =>
        run(quote(tracksTable.filter(t => t.id == lift(id) || t.path == lift(pathFromId(id)))))

  override def findFile(id: TrackID): Future[Option[Path]] =
    track(id).map: maybeTrack =>
      maybeTrack
        .flatMap(t => local.findAbsoluteNew(t.path))
        .orElse(local.findAbsoluteLegacy(id))

  def insertFolders(fs: Seq[DataFolder]): Future[Long] =
    performAsync(s"Insert ${fs.length} folders"):
      fs.map(f => run(foldersTable.insertValue(f))).sum

  def insertTracks(ts: Seq[DataTrack]): Future[Long] = performAsync(s"Insert ${ts.length} tracks"):
    ts.map(t => run(tracksTable.insertValue(t))).sum

  private def pathFromId(id: Identifier): UnixPath = UnixPath.fromRaw(PimpEnc.decodeId(id))
