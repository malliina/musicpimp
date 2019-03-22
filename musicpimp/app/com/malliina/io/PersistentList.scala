package com.malliina.io

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
    val remaining = filterNot(item, saved)
    val changed = saved != remaining
    if (saved != remaining) {
      persist(remaining)
    }
    changed
  }

  def filterNot(elem: T, others: Seq[T]) = others.filter(_ != elem)

  def get(): Seq[T] = load()

  protected def persist(items: Seq[T]): Unit

  protected def load(): Seq[T]
}
