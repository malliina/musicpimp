package com.mle.musicpimp.util

import java.nio.file.{Files, Path}
import play.api.libs.json.Format
import play.api.libs.json.Json._

trait Distinctness[T] extends PersistentList[T] {
  abstract override def add(item: T): Boolean = {
    val saved = load()
    val alreadyContains = saved.contains(item)
    if (!alreadyContains) {
      persist(saved :+ item)
    }
    !alreadyContains
  }
}

trait PersistentList[T] {
  /**
   *
   * @return true if the collection was modified, false otherwise
   */
  def add(item: T): Boolean = {
    persist(load() :+ item)
    true
  }

  /**
   *
   * @return true if the collection was modified, false otherwise
   */
  def remove(item: T): Boolean = {
    val saved = load()
    val remaining = saved.filter(_ != item)
    val changed = saved != remaining
    if (saved != remaining) {
      persist(remaining)
    }
    changed
  }

  def get(): Seq[T] = load()

  protected def persist(items: Seq[T]): Unit

  protected def load(): Seq[T]
}
