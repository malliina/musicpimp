package com.mle.musicpimp.db

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.musicpimp.library.Library
import com.mle.musicpimp.util.FileUtil
import com.mle.play.concurrent.ExecutionContexts.synchronousIO
import com.mle.rx.Observables
import com.mle.util.Log
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Try

/**
 * Keeps the music library index up-to-date with respect to the actual file system.
 *
 * Indexes the music library if it changes. Performs consistency checks during object initialization and every six hours
 * from then on, indexing if necessary.
 *
 * @author Michael
 */
object Indexer extends Log {
  val indexFile = FileUtil.localPath("files7.cache")
  val indexInterval = 6.hours
  val timer = Observable.interval(indexInterval).subscribe(_ => indexIfNecessary())
  private val ongoingIndexings = Subject[Observable[Long]]()
  val ongoing: Observable[Observable[Long]] = ongoingIndexings

  indexIfNecessary()

  def init() = ()

  def indexIfNecessary(): Observable[Long] = Observables hot {
    log info "Indexing if necessary..."
    val actualObs = currentFileCount
    val saved = savedFileCount
    actualObs flatMap (actual => {
      if (actual != saved) {
        log info s"Saved file count of $saved differs from actual file count of $actual, indexing..."
        remembered(index(actual))
      } else {
        log info s"There are $savedFileCount files in the library. No change since last time, not indexing."
        Observable.empty
      }
    })
  }

  /**
   * Indexes the music library.
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
  def index(): Observable[Long] = remembered(Observables.hot(currentFileCount flatMap index))

  private def index(count: Int): Observable[Long] = {
    try {
      FileUtilities.stringToFile(count.toString, indexFile)
      Observable.just(0L) ++ PimpDb.refreshIndex()
    } catch {
      case e: Exception => Observable.error(e)
    }
  }

  private def remembered(observable: Observable[Long]) = {
    ongoingIndexings onNext observable
    observable
  }

  private def savedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def currentFileCountFuture = Future(Library.trackFiles.size)

  private def currentFileCount: Observable[Int] = Observable.from(currentFileCountFuture)
}
