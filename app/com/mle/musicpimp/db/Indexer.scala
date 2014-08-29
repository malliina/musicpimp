package com.mle.musicpimp.db

import com.mle.musicpimp.library.Library
import com.mle.musicpimp.util.FileUtil
import com.mle.util.FileImplicits.StorageFile
import com.mle.util.{FileUtilities, Log}
import rx.lang.scala.Observable

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
    if (fileCountChanged) index()
    else Observable.empty
  }

  def index() = {
    FileUtilities.stringToFile(currentFileCount.toString, indexFile)
    PimpDb.refreshIndex()
  }

  private def fileCountChanged: Boolean = savedFileCount != currentFileCount

  private def savedFileCount = Try(FileUtilities.fileToString(indexFile).toInt) getOrElse 0

  private def currentFileCount = Library.trackFiles.size
}