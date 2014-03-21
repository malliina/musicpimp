package com.mle.push

import com.mle.util._
import com.mle.musicpimp.util.{FileBackedSet, PersistentList}

/**
 *
 * @author mle
 */
object PushUrls
  extends FileBackedSet[PushUrl](FileUtilities.pathTo("push.json"))
  with LoggingList

trait LoggingList extends PersistentList[PushUrl] with Log {
  abstract override def add(item: PushUrl): Boolean = {
    val added = super.add(item)
    if (added) {
      log.info(s"Added push URL: $item")
    }
    added
  }

  abstract override def remove(item: PushUrl): Boolean = {
    val removed = super.remove(item)
    if (removed) {
      log.info(s"Removed push URL: $item")
    }
    removed
  }
}

