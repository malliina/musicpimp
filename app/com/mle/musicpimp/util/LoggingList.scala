package com.mle.musicpimp.util

import com.mle.util.Log

/**
 * @author Michael
 */
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
