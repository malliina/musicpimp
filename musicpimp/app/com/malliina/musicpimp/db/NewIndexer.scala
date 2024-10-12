package com.malliina.musicpimp.db

import com.malliina.musicpimp.db.NewIndexer.log
import com.malliina.musicpimp.library.FileStreams
import io.getquill.*
import play.api.Logger

import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.concurrent.{Await, ExecutionContext, Future}

object NewIndexer:
  private val log = Logger(getClass)

  def apply(db: PimpMySQL): NewIndexer = new NewIndexer(db)

class NewIndexer(val db: PimpMySQL):
  implicit val ec: ExecutionContext = db.ec
  import db.*

  val tempFoldersTable = quote(querySchema[TempFolder]("TEMP_FOLDERS"))
  val tempTracksTable = quote(querySchema[TempTrack]("TEMP_TRACKS"))

  def runIndexer(
    library: FileStreams
  )(onFileCountUpdate: Long => Future[Unit]): Future[IndexResult] =
    wrapTask("Indexer"):
      log.info("Indexing...")

      var fileCount = 0L

      val musicFolders = library.folderStream.toList
      val firstIdsDeletion = run(tempFoldersTable.delete)
      val updateIO = musicFolders.map(mf => upsertFolder(mf))
      val idInsertion =
        musicFolders
          .map(mf => TempFolder(mf.id))
          .map(tf => run(tempFoldersTable.insertValue(lift(tf))))
      val tempIds = run(tempFoldersTable.map(_.id))
      val foldersDeletion =
        run(foldersTable.filter(f => !tempFoldersTable.map(_.id).contains(f.id)).delete)
      val secondIdsDeletion = run(tempFoldersTable.delete)
      val _ = run(tempTracksTable.delete)

      upsertAll(library.dataTrackStream): chunkSize =>
        fileCount += chunkSize
        onFileCountUpdate(fileCount)

      val tracksDeleted = run(
        tracksTable.filter(t => !tempTracksTable.map(_.id).contains(t.id)).delete
      )
      val _ = run(tempFoldersTable.delete)
      IndexResult(fileCount, foldersDeletion.toInt, tracksDeleted.toInt)

  def upsertFolder(folder: DataFolder) =
    val isEmpty = run(foldersTable.filter(_.id == lift(folder.id))).isEmpty
    if isEmpty then run(foldersTable.insertValue(lift(folder)))
    else
      run(
        foldersTable
          .filter(_.id == lift(folder.id))
          .update(
            _.title -> lift(folder.title),
            _.parent -> lift(folder.parent),
            _.path -> lift(folder.path)
          )
      )

  private def upsertAll(tracks: LazyList[DataTrack])(progress: Int => Unit): Unit =
    tracks
      .grouped(100)
      .foreach: chunk =>
        val ts = chunk.toList
        val upserts = ts.map(tra => upsertTrack(tra))
        val in = ts.map(t => TempTrack(t.id)).map(tt => run(tempTracksTable.insertValue(lift(tt))))
//        await(performAsync("Insert chunk")(task), 1.hour)
        progress(chunk.size)

  private def upsertTrack(track: DataTrack): Long =
    val isEmpty = run(tracksTable.filter(_.id == lift(track.id))).isEmpty
    if isEmpty then run(tracksTable.insertValue(lift(track)))
    else
      run(
        tracksTable
          .filter(_.id == lift(track.id))
          .update(
            _.title -> lift(track.title),
            _.artist -> lift(track.artist),
            _.album -> lift(track.album),
            _.duration -> lift(track.duration),
            _.size -> lift(track.size),
            _.path -> lift(track.path),
            _.folder -> lift(track.folder)
          )
      )

  def await[T](f: Future[T], dur: FiniteDuration = 5.seconds): T =
    Await.result(f, dur)
