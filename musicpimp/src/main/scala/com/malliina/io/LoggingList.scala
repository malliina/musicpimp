package com.malliina.io

import com.malliina.io.LoggingList.log
import org.slf4j.LoggerFactory

trait LoggingList[T] extends PersistentList[T]:
  abstract override def add(item: T): Boolean =
    val added = super.add(item)
    if added then log.info(s"Added item: $item")
    added

  abstract override def remove(item: T): Boolean =
    val removed = super.remove(item)
    if removed then log.info(s"Removed item: $item")
    removed

object LoggingList:
  private val log = LoggerFactory.getLogger(getClass)
