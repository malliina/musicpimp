package com.malliina.musicpimp.db

import com.malliina.musicpimp.library.FileStreams
import play.api.Logger

import scala.concurrent.{Await, ExecutionContext, Future}
import NewIndexer.log

import concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

object NewIndexer {
  private val log = Logger(getClass)

  def apply(db: PimpMySQL): NewIndexer = new NewIndexer(db)
}

class NewIndexer(val db: PimpMySQL) {
  implicit val ec: ExecutionContext = db.ec
  import db._

  val tempFoldersTable = quote(querySchema[TempFolder]("TEMP_FOLDERS"))
  val tempTracksTable = quote(querySchema[TempTrack]("TEMP_TRACKS"))

  def runIndexer(
    library: FileStreams
  )(onFileCountUpdate: Long => Future[Unit]): Future[IndexResult] = {
    log info "Indexing..."

    var fileCount = 0L

    val musicFolders = library.folderStream.toList
    val updateFolders = for {
      firstIdsDeletion <- runIO(tempFoldersTable.delete)
      updateIO <- IO.traverse(musicFolders)(mf => upsertFolder(mf))
      idInsertion <- runIO(
        liftQuery(musicFolders.map(mf => TempFolder(mf.id)))
          .foreach(tf => tempFoldersTable.insert(tf))
      )
      foldersDeletion <- runIO(
        foldersTable.filter(f => !tempFoldersTable.map(_.id).contains(f.id)).delete
      )
      secondIdsDeletion <- runIO(tempFoldersTable.delete)
    } yield foldersDeletion

    def upsertTracks(): Unit =
      upsertAll(library.dataTrackStream) { chunkSize =>
        fileCount += chunkSize
        onFileCountUpdate(fileCount)
      }

    val deleteNonExistentTracks = for {
      tracksDeleted <- runIO(
        tracksTable.filter(t => !tempTracksTable.map(_.id).contains(t.id)).delete
      )
      _ <- runIO(tempFoldersTable.delete)
    } yield tracksDeleted

    for {
      fs <- performAsync("Update folders")(updateFolders)
      deletion <- performAsync("Delete tracks")(runIO(tempTracksTable.delete))
      ups = upsertTracks()
      ts <- performAsync("Delete nonexistent tracks")(deleteNonExistentTracks)
    } yield IndexResult(fileCount, fs.toInt, ts.toInt)
  }

  def upsertFolder(folder: DataFolder) =
    for {
      isEmpty <- runIO(foldersTable.filter(_.id == lift(folder.id)).isEmpty)
      inOrUp <- if (isEmpty) runIO(foldersTable.insert(lift(folder)))
      else
        runIO(
          foldersTable
            .filter(_.id == lift(folder.id))
            .update(
              _.title -> lift(folder.title),
              _.parent -> lift(folder.parent),
              _.path -> lift(folder.path)
            )
        )
    } yield inOrUp

  def upsertAll(tracks: LazyList[DataTrack])(progress: Int => Unit): Unit =
    tracks.grouped(100).foreach { chunk =>
      val ts = chunk.toList
      val task = for {
        upserts <- IO.traverse(ts)(tra => upsertTrack(tra))
        in <- runIO(
          liftQuery(ts.map(t => TempTrack(t.id))).foreach(t => tempTracksTable.insert(t))
        )
      } yield in
      await(performAsync("Insert chunk")(task), 1.hour)
      progress(chunk.size)
    }

  def upsertTrack(track: DataTrack): IO[Long, Effect.Read with Effect.Write] =
    for {
      isEmpty <- runIO(tracksTable.filter(_.id == lift(track.id)).isEmpty)
      inOrUp <- if (isEmpty) runIO(tracksTable.insert(lift(track)))
      else
        runIO(
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
    } yield inOrUp

  def await[T](f: Future[T], dur: FiniteDuration = 5.seconds): T =
    Await.result(f, dur)
}
