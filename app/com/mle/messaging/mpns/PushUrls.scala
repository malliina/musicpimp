package com.mle.messaging.mpns

import com.mle.util._
import com.mle.musicpimp.util.{FileBackedSet, PersistentList}
import play.api.libs.json.Format

/**
 *
 * @author mle
 */
object PushUrls extends PushSet[PushUrl]("push.json")

abstract class PushSet[T](file: String)(implicit format: Format[T]) extends FileBackedSet[T](FileUtilities pathTo file) with LoggingList[T]

trait LoggingList[T] extends PersistentList[T] with Log {
  abstract override def add(item: T): Boolean = {
    val added = super.add(item)
    if (added) {
      log.info(s"Added push URL: $item")
    }
    added
  }

  abstract override def remove(item: T): Boolean = {
    val removed = super.remove(item)
    if (removed) {
      log.info(s"Removed push URL: $item")
    }
    removed
  }
}

