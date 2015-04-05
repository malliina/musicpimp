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
  val indexFile = FileUtil.pimpHomeDir / "files7.cache"
  val indexInterval = 6.hours
  indexIfNecessary()
  val timer = Observable.interval(indexInterval).subscribe(_ => indexIfNecessary())

  private val ongoingIndexings = Subject[Observable[Long]]()
  val ongoing: Observable[Observable[Long]] = ongoingIndexings

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
        log info s"There are still $savedFileCount files in the library. No change since last time, not indexing."
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
    val fileCounter = futObs(FileUtilities.stringToFile(count.toString, indexFile)).map(_ => 0L)
    log debug s"Indexing with count: $count"
    fileCounter ++ PimpDb.refreshIndex()
  }

  private def remembered(observable: Observable[Long]) = {
    ongoingIndexings onNext observable
    observable
  }

  private def savedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def currentFileCountSync = Library.trackFiles.size

  private def currentFileCount: Observable[Int] = futObs(currentFileCountSync)

  private def futObs[T](body: => T) = Observable.from(Future(body))
}