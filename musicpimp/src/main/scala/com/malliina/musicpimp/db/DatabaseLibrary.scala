package com.malliina.musicpimp.db

import cats.data.NonEmptyList
import cats.effect.{Async, IO}
import cats.implicits.{toFlatMapOps, toFunctorOps, toTraverseOps}
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.audio.{PimpEnc, TrackMeta}
import com.malliina.musicpimp.library.{Library, LocalTrack, MusicFolder, MusicLibrary}
import com.malliina.musicpimp.models.{FolderID, Identifier, TrackID}
import com.malliina.values.UnixPath
import doobie.implicits.*
import doobie.util.fragments.{in, or}

import java.nio.file.Path

class DatabaseLibrary[F[_]: Async](db: DoobieDatabase[F], local: Library)
  extends MusicLibrary[F]
  with DoobieMappings:
  val F = Async[F]

  def rootFolder: F[MusicFolder] =
    folder(Library.RootId).map(_.getOrElse(MusicFolder.empty))

  def folder(id: FolderID): F[Option[MusicFolder]] = db.run:
    val path = pathFromId(id)
    val parent = sql"""select F.ID, F.TITLE, F.PATH, F.PARENT
                       from FOLDERS F
                       where F.ID = $id or F.PATH = $path"""
      .query[DataFolder]
      .option
    val folders = sql"""select F.ID, F.TITLE, F.PATH, F.PARENT
                        from FOLDERS F
                        join FOLDERS P on F.PARENT = P.ID
                        where F.ID <> ${Library.RootId} and (P.ID = $id or P.PATH = $path)
                        order by F.TITLE"""
      .query[DataFolder]
      .to[List]
    val tracks = sql"""select T.ID, T.TITLE, T.ARTIST, T.ALBUM, T.DURATION, T.SIZE, T.PATH, T.FOLDER
                       from TRACKS T
                       join FOLDERS F on T.FOLDER = F.ID
                       where F.ID = $id or F.PATH = $path
                       order by T.TITLE, T.ARTIST"""
      .query[DataTrack]
      .to[List]
    for
      p <- parent
      ts <- tracks
      fs <- folders
    yield p.map: folder =>
      MusicFolder(folder, fs, ts)

  def tracksIn(id: FolderID): F[Option[List[TrackMeta]]] =
    folder(id).flatMap: maybeFolder =>
      maybeFolder
        .map: folder =>
          folder.folders.toList
            .traverse: sub =>
              tracksInOrEmpty(sub.id)
            .map: subs =>
              folder.tracks.toList ++ subs.flatten
        .map(_.map(Option.apply))
        .getOrElse(F.pure(None))

  def track(id: TrackID): F[Option[TrackMeta]] =
    tracksFor(NonEmptyList.of(id)).map(_.headOption)

  def tracks(ids: NonEmptyList[TrackID]): F[List[LocalTrack]] =
    tracksFor(ids).map(_.map(local.toLocal))

  def meta(id: TrackID): F[Option[LocalTrack]] =
    track(id).map(_.map(local.toLocal))

  def findFile(id: TrackID): F[Option[Path]] =
    track(id).map: maybeTrack =>
      maybeTrack
        .flatMap(t => local.findAbsoluteNew(t.path))
        .orElse(local.findAbsoluteLegacy(id))

  def insertFolders(fs: Seq[DataFolder]): F[Long] = db.run:
    fs.toList
      .traverse: f =>
        sql"""insert into FOLDERS(ID, TITLE, PATH, PARENT)
              values (${f.id}, ${f.title}, ${f.path}, ${f.parent})""".update.run
      .map(_.sum.toLong)

  def insertTracks(ts: Seq[DataTrack]): F[Long] = db.run:
    ts.toList
      .traverse: t =>
        sql"""insert into TRACKS(ID, TITLE, ARTIST, ALBUM, DURATION, SIZE, PATH, FOLDER)
              values (${t.id}, ${t.title}, ${t.artist}, ${t.album}, ${t.duration}, ${t.size}, ${t.path}, ${t.folder})""".update.run
      .map(_.sum.toLong)

  def deleteTracks = db.run:
    sql"""delete from TRACKS""".update.run

  def deleteFolders = db.run:
    sql"""delete from FOLDERS""".update.run

  private def tracksInOrEmpty(id: FolderID) = tracksIn(id).map(_.getOrElse(Nil))

  private def tracksFor(ids: NonEmptyList[TrackID]): F[List[DataTrack]] = db.run:
    val condition = or(in(fr"T.ID", ids), in(fr"T.PATH", ids.map(pathFromId)))
    sql"""select T.ID, T.TITLE, T.ARTIST, T.ALBUM, T.DURATION, T.SIZE, T.PATH, T.FOLDER
          from TRACKS T
          where $condition"""
      .query[DataTrack]
      .to[List]

  private def pathFromId(id: Identifier): UnixPath = UnixPath.fromRaw(PimpEnc.decodeId(id))
