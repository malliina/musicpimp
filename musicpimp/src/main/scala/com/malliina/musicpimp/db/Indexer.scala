package com.malliina.musicpimp.db

import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps, toFlatMapOps, toFunctorOps}
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.db.Indexer.log
import com.malliina.musicpimp.library.FileLibrary
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.AppLogger
import fs2.Stream
import fs2.concurrent.Topic

import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.util.Try

enum IndexEvent:
  case Started, Finished
  case Progress(files: Long)
  case Errored(t: Throwable)

object Indexer:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](library: FileLibrary, indexer: DoobieIndexer[F]) =
    for
      indexingHub <- Topic[F, Stream[F, Long]]
      updates <- Topic[F, IndexEvent]
      halts <- Topic[F, Boolean]
    yield Indexer(library, indexer, indexingHub, updates, halts)

/** Keeps the music library index up-to-date with respect to the actual file system.
  *
  * Indexes the music library if it changes. Runs when `init()` is first called and every six hours
  * from then on.
  */
class Indexer[F[_]: Async](
  library: FileLibrary,
  indexer: DoobieIndexer[F],
  indexingHub: Topic[F, Stream[F, Long]],
  updatesSink: Topic[F, IndexEvent],
  halts: Topic[F, Boolean]
):
  val F = Async[F]
  private val indexFile = FileUtil.localPath("files7.cache")
  private val indexInterval = 6.hours
  private val indexUpdates = indexingHub
    .subscribe(100)
    .flatMap: job =>
      Stream.eval(F.delay(log.info("Got indexing job."))) >>
        Stream(IndexEvent.Started)
          .append(job.map(l => IndexEvent.Progress(l)))
          .append(Stream(IndexEvent.Finished))
          .handleError(t => IndexEvent.Errored(t))
  val updates: Stream[F, IndexEvent] = updatesSink.subscribe(100)
  val events: Stream[F, Unit] = indexRegularly
    .concurrently(indexUpdates.map(upd => updatesSink.publish1(upd).void))
    .interruptWhen(halts.subscribe(100))

  private def indexRegularly: Stream[F, Unit] =
    Stream.eval(F.delay(log.info("Init indexer..."))) >> Stream
      .awakeEvery(indexInterval)
      .evalMap: _ =>
        F.delay(log.info("Queueing indexing.")) >>
          indexIfNecessary()
      .handleError: e =>
        F.delay(log.error(s"Failed to index.", e))

  private def indexIfNecessary(): F[Unit] =
    val saved = loadSavedFileCount
    calculateFileCount()
      .flatMap: actual =>
        if actual != saved then
          saveFileCount(actual)
            .map: _ =>
              log.info(
                s"Saved file count of $saved differs from actual file count of $actual, indexing..."
              )
            .recover:
              case e: Exception =>
                log.error(s"Unable to save file count of $actual", e)
            .flatMap: _ =>
              submitIndexing()
        else
          log.info(
            s"There are $actual files in the library. No change since last time, not indexing."
          )
          F.unit

  /** Indexes the music library.
    *
    * @return
    *   indexing progress
    */
  private def submitIndexing(): F[Unit] =
    indexingHub
      .publish1(refreshIndex())
      .void
      .flatTap: _ =>
        F.delay(log.info("Submitted indexing request."))

  /** Starts indexing on a background thread and returns a [[Stream]] with progress updates.
    *
    * This algorithm adds new tracks and folders to the index, and removes tracks and folders that
    * no longer exist in the library.
    *
    * Implementation:
    *
    * 1) Upsert all folders to the folders table, based on the (authoritative) library 2) Put all
    * existing folder IDs also into a temporary table (also based on the library) 3) Delete all
    * folders which don't have IDs in the temporary table 4) Delete the temporary table 5) Do the
    * same thing for tracks (go to step 1)
    *
    * @return
    *   progress: total amount of files indexed
    */
  private def refreshIndex(): Stream[F, Long] =
    Stream
      .eval(Topic[F, Long])
      .flatMap: hub =>
        val start = System.currentTimeMillis()
        val task = indexer
          .runIndexer(library): fileCount =>
            log.info(s"File count at $fileCount...")
            hub.publish1(fileCount).void
          .map: result =>
            val end = System.currentTimeMillis()
            val duration = (end - start).millis
            log.info(
              s"Indexing complete in $duration. Indexed ${result.totalFiles} files, " +
                s"purged ${result.foldersPurged} folders and ${result.tracksPurged} files."
            )
          .handleError: e =>
            log.error(s"Indexing failed.", e)
        hub.subscribe(100).concurrently(Stream.eval(task))

  def submitIndexAndSave(): F[Unit] =
    submitIndexing().flatMap(_ => countAndSaveFiles().void)

  private def countAndSaveFiles(): F[Int] =
    for
      count <- calculateFileCount()
      _ <- saveFileCount(count)
    yield count

  private def saveFileCount(count: Int) = F.delay:
    FileUtilities.stringToFile(count.toString, indexFile)

  private def loadSavedFileCount = Try(FileUtilities.fileToString(indexFile).toInt).getOrElse(0)

  private def calculateFileCount() = F.delay:
    log.info(s"Calculating file count...")
    library.trackFiles.size

  def close(): F[Unit] = halts.publish1(true).void
