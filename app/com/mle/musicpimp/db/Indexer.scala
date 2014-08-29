package com.mle.musicpimp.db

import com.mle.musicpimp.library.Library
import com.mle.musicpimp.util.FileUtil
import com.mle.util.FileImplicits.StorageFile
import com.mle.util.{FileUtilities, Log}
import rx.lang.scala.Observable
import com.mle.play.concurrent.ExecutionContexts.synchronousIO

import scala.concurrent.Future
import scala.concurrent.duration.DurationLong
import scala.util.Try

/**
 * Keeps the music library index up-to-date with respect to the actual file system.
 *
 * Indexes the music library if it changes. Performs consistency checks during `init()` and every six hours from then
 * on, indexing if necessary.
 *
 * @author Michael
 */
object Indexer extends Log {
  val indexFile = FileUtil.pimpHomeDir / "files.cache"
  val indexInterval = 6.hours
  indexIfNecessary()
  val timer = Observable.interval(indexInterval).subscribe(_ => indexIfNecessary())

  def init() = ()

  def indexIfNecessary(): Observable[Long] = {
    val actual = currentFileCount
    val saved = savedFileCount
    if (actual != saved) {
      log info s"Saved file count of $saved differs from actual file count of $actual, indexing..."
      index()
    } else {
      log info s"There are still $savedFileCount files in the library. The index is up-to-date."
      Observable.empty
    }
  }

  def index(): Observable[Long] = {
    val fileCounter = Observable.from(Future(FileUtilities.stringToFile(currentFileCount.toString, indexFile))).map(_ => 0L)
    fileCounter ++ PimpDb.refreshIndex()
  }

  private def savedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def currentFileCount = Library.trackFiles.size
}