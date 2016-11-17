package com.malliina.musicpimp.db

import com.malliina.concurrent.ExecutionContexts.cached
import com.malliina.file.FileUtilities
import com.malliina.musicpimp.db.Indexer.log
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.util.FileUtil
import com.malliina.rx.Observables
import play.api.Logger
import rx.lang.scala.{Observable, Subject, Subscription}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try

/** Keeps the music library index up-to-date with respect to the actual file system.
  *
  * Indexes the music library if it changes. Runs when `init()` is first called and
  * every six hours from then on.
  */
class Indexer(db: PimpDb) {
  val indexFile = FileUtil.localPath("files7.cache")
  val indexInterval = 6.hours
  private var timer: Option[Subscription] = None
  private val ongoingIndexings = Subject[Observable[Long]]().toSerialized
  val ongoing: Observable[Observable[Long]] = ongoingIndexings

  def init() = {
    timer.foreach(_.unsubscribe())
    val sub = Observable.interval(indexInterval).subscribe(_ => indexIfNecessary())
    timer = Option(sub)
    indexIfNecessary()
  }

  def indexIfNecessary(): Observable[Long] = Observables hot {
    log info "Indexing if necessary..."
    val actualObs = calculateFileCount
    val saved = loadSavedFileCount
    actualObs flatMap (actual => {
      if (actual != saved) {
        saveFileCount(actual).recover {
          case e: Exception =>
            log.error(s"Unable to save file count of $actual", e)
        }
        log info s"Saved file count of $saved differs from actual file count of $actual, indexing..."
        index()
      } else {
        log info s"There are $actual files in the library. No change since last time, not indexing."
        Observable.empty
      }
    })
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
  def index(): Observable[Long] = {
    val observable = Observables.hot(db.refreshIndex())
    ongoingIndexings onNext observable
    observable
  }

  def indexAndSave(): Observable[Long] = {
    val ret = index()
    countAndSaveFiles()
    ret
  }

  private def countAndSaveFiles(): Future[Int] = for {
    count <- currentFileCountFuture
    _ <- saveFileCount(count)
  } yield count

  private def saveFileCount(count: Int) = Future(FileUtilities.stringToFile(count.toString, indexFile))

  private def loadSavedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def currentFileCountFuture = Future {
    log info s"Calculating file count..."
    Library.trackFiles.size
  }

  private def calculateFileCount: Observable[Int] = Observable.from(currentFileCountFuture)
}

object Indexer {
  private val log = Logger(getClass)
}
