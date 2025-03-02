package com.malliina.musicpimp.db

import cats.effect.Async
import cats.effect.implicits.concurrentParTraverseOps
import cats.implicits.{toFlatMapOps, toFunctorOps, toTraverseOps}
import com.malliina.database.DoobieDatabase
import com.malliina.musicpimp.library.FileStreams
import doobie.implicits.toSqlInterpolator

class DoobieIndexer[F[_]: Async](db: DoobieDatabase[F]) extends DoobieMappings:
  def runIndexer(
    library: FileStreams
  )(onFileCountUpdate: Long => F[Unit]): F[IndexResult] =
    var fileCount = 0L

    val foldersPrep = db.run:
      val musicFolders = library.folderStream.toList

      for
        _ <- sql"delete from TEMP_FOLDERS".update.run
        _ <- musicFolders.traverse(f => upsertFolder(f))
        _ <- musicFolders.traverse(f =>
          sql"insert into TEMP_FOLDERS(ID) values(${f.id})".update.run
        )
        foldersDeletion <-
          sql"delete from FOLDERS where ID not in (select ID from TEMP_FOLDERS)".update.run
        _ <- sql"delete from TEMP_FOLDERS".update.run
        _ <- sql"delete from TEMP_TRACKS".update.run
      yield foldersDeletion
    val trackInsertion = upsertAll(library.dataTrackStream): chunkSize =>
      fileCount += chunkSize
      onFileCountUpdate(fileCount)
    for
      foldersDeletion <- foldersPrep
      tracksInsertion <- upsertAll(library.dataTrackStream): chunkSize =>
        fileCount += chunkSize
        onFileCountUpdate(fileCount)
      tracksDeleted <- db.run:
        for
          tracksDeleted <-
            sql"delete from TRACKS where ID not in (select ID from TEMP_TRACKS)".update.run
          _ <- sql"delete from TEMP_FOLDERS".update.run
        yield tracksDeleted
    yield IndexResult(fileCount, foldersDeletion, tracksDeleted)

  private def upsertFolder(folder: DataFolder) =
    for
      isNonEmpty <- sql"select exists(select ID from FOLDERS F where F.ID = ${folder.id})"
        .query[Boolean]
        .unique
      upsertSql =
        if isNonEmpty then
          sql"""update FOLDERS
                                 set TITLE = ${folder.title}, PATH = ${folder.path}, PARENT = ${folder.parent}
                                 where ID = ${folder.id}"""
        else sql"""insert into FOLDERS(ID, TITLE, PATH, PARENT)
                   values (${folder.id}, ${folder.title}, ${folder.path}, ${folder.parent})"""
      upsert <- upsertSql.update.run
    yield upsert

  private def upsertTrack(t: DataTrack) =
    for
      isNonEmpty <- sql"select exists(select ID from TRACKS T where T.ID = ${t.id})"
        .query[Boolean]
        .unique
      updateSql =
        if isNonEmpty then
          sql"""update TRACKS set TITLE = ${t.title}, ARTIST = ${t.artist}, ALBUM = ${t.album}, DURATION = ${t.duration}, SIZE = ${t.size}, PATH = ${t.path}, FOLDER = ${t.folder} where ID = ${t.id}"""
        else
          sql"""insert into TRACKS(ID, TITLE, ARTIST, ALBUM, DURATION, SIZE, PATH, FOLDER)
                   values (${t.id}, ${t.title}, ${t.artist}, ${t.album}, ${t.duration}, ${t.size}, ${t.path}, ${t.folder})"""
      upsert <- updateSql.update.run
    yield upsert

  private def upsertAll(tracks: LazyList[DataTrack], acc: List[Int] = Nil)(
    progress: Int => F[Unit]
  ): F[List[Int]] =
    if tracks.isEmpty then Async[F].pure(acc)
    else
      val (chunk, tail) = tracks.splitAt(100)
      val batch = chunk.toList
        .parTraverseN(1): t =>
          db.run:
            for
              upsert <- upsertTrack(t)
              temp <- sql"insert into TEMP_TRACKS(ID) values (${t.id})".update.run
            yield upsert
      for
        ints <- batch
        _ <- progress(chunk.size)
        rest <- upsertAll(tail, acc ++ ints)(progress)
      yield rest
