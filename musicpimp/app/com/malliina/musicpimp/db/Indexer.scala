package com.malliina.musicpimp.db

import akka.NotUsed
import akka.actor.{Cancellable, PoisonPill, Scheduler}
import akka.stream.Materializer
import akka.stream.scaladsl.{BroadcastHub, Keep, Sink, Source}
import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.db.Indexer.log
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.util.FileUtil
import com.malliina.rx.Sources
import play.api.Logger
import rx.lang.scala.Observable

import scala.concurrent.Future
import scala.concurrent.duration.{DurationInt, DurationLong}
import scala.util.Try

object Indexer {
  private val log = Logger(getClass)
}

/** Keeps the music library index up-to-date with respect to the actual file system.
  *
  * Indexes the music library if it changes. Runs when `init()` is first called and
  * every six hours from then on.
  */
class Indexer(db: PimpDb, s: Scheduler)(implicit val mat: Materializer) {
  val indexFile = FileUtil.localPath("files7.cache")
  val indexInterval = 6.hours
//  val indexInterval = 15.seconds
  private var timer: Option[Cancellable] = None
  private val (indexingTarget, indexings) = Sources.connected[Source[Long, NotUsed]]()
  val ongoing: Source[Source[Long, NotUsed], NotUsed] = indexings

  def init(): Unit = {
    log.info("Init indexer...")
    val (cancellable, _) = Source
      .tick(1.second, indexInterval, 0)
      .map(_ => indexIfNecessary())
      .toMat(Sink.foreach(_ => log.info("Queueing indexing.")))(Keep.both)
      .run()
    timer.foreach(_.cancel())
    timer = Option(cancellable)
  }

  def indexIfNecessary(): Source[Long, NotUsed] = {
    log info "Indexing if necessary..."
    val actualObs = calculateFileCount()
    val saved = loadSavedFileCount
    val task = Source.fromFuture(actualObs) flatMapConcat { actual =>
      if (actual != saved) {
        Source.fromFuture {
          saveFileCount(actual).map { _ =>
            log info s"Saved file count of $saved differs from actual file count of $actual, indexing..."
          }.recover {
            case e: Exception =>
              log.error(s"Unable to save file count of $actual", e)
          }
        }.flatMapConcat { _ =>
          index()
        }
      } else {
        log info s"There are $actual files in the library. No change since last time, not indexing."
        Source.empty
      }
    }
    task.toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right).run()
  }

  /** Indexes the music library.
    *
    * Note that this is a hot [[Observable]] for two reasons: we want the indexing to run regardless of whether there are
    * any subscribers, and we want it to run only once. All subscribers to the returned [[Observable]] will share the
    * same stream of events.
    *
    * I think an observable should be hot when the event stream in itself is side-effecting, whereas normally we
    * subscribe to cold observables to cause side effects.
    *
    * @return a hot [[Observable]] with indexing progress
    */
  def index(): Source[Long, NotUsed] = {
    val task = refreshIndex().toMat(BroadcastHub.sink(bufferSize = 256))(Keep.right).run()
    indexingTarget ! task
    task
  }

  /** Starts indexing on a background thread and returns a [[Source]] with progress updates.
    *
    * This algorithm adds new tracks and folders to the index, and removes tracks and folders that no longer exist in
    * the library.
    *
    * Implementation:
    *
    * 1) Upsert all folders to the folders table, based on the (authoritative) library
    * 2) Put all existing folder IDs also into a temporary table (also based on the library)
    * 3) Delete all folders which don't have IDs in the temporary table
    * 4) Delete the temporary table
    * 5) Do the same thing for tracks (go to step 1)
    *
    * @return progress: total amount of files indexed
    */
  private def refreshIndex(): Source[Long, NotUsed] = {
    val (target, source) = Sources.connected[Long]()
    val start = System.currentTimeMillis()
    db.runIndexer { fileCount =>
      log.info(s"File count at $fileCount...")
      Future.successful(target ! fileCount)
    }.map { result =>
      val end = System.currentTimeMillis()
      val duration = (end - start).millis
      target ! PoisonPill
      log info s"Indexing complete in $duration. Indexed ${result.totalFiles} files, " +
        s"purged ${result.foldersPurged} folders and ${result.tracksPurged} files."
    }.recover {
      case e =>
        log.error(s"Indexing failed.", e)
        target ! PoisonPill
    }
    source
  }

  def indexAndSave(): Source[Long, NotUsed] = {
    val ret = index()
    countAndSaveFiles()
    ret
  }

//  def index(): Observable[Long] = {
//    val observable = Observables.hot(db.refreshIndex())
//    ongoingIndexings onNext observable
//    observable
//  }
//
//  def indexAndSave(): Observable[Long] = {
//    val ret = index()
//    countAndSaveFiles()
//    ret
//  }

  private def countAndSaveFiles(): Future[Int] = for {
    count <- calculateFileCount()
    _ <- saveFileCount(count)
  } yield count

  private def saveFileCount(count: Int) = Future(
    FileUtilities.stringToFile(count.toString, indexFile))

  private def loadSavedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def calculateFileCount() = Future {
    log info s"Calculating file count..."
    Library.trackFiles.size
  }
}
