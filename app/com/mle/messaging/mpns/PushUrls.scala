package com.mle.messaging.mpns

import com.mle.util._
import com.mle.musicpimp.util.{FileBackedSet, PersistentList}
import play.api.libs.json.Format

/**
 *
 * @author mle
 */
object PushUrls extends PushSet[PushUrl]("push.json") {
  override protected def id(elem: PushUrl): String = elem.url
}

abstract class PushSet[T](file: String)(implicit format: Format[T]) extends FileBackedSet[T](FileUtilities pathTo file) with LoggingList[T] {
  protected def id(elem: T): String

  def withID(elemID: String): Option[T] = get().find(e => id(e) == elemID)

  def removeID(id: String) = withID(id).foreach(remove)
}

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

