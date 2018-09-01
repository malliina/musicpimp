package com.malliina.play.io

import com.malliina.play.io.LoggingList.log
import play.api.Logger

trait LoggingList[T] extends PersistentList[T] {
  abstract override def add(item: T): Boolean = {
    val added = super.add(item)
    if (added) {
      log.info(s"Added item: $item")
    }
    added
  }

  abstract override def remove(item: T): Boolean = {
    val removed = super.remove(item)
    if (removed) {
      log.info(s"Removed item: $item")
    }
    removed
  }
}

object LoggingList {
  private val log = Logger(getClass)
}
