package com.malliina.io

trait Distinctness[T] extends PersistentList[T] {
  abstract override def add(item: T): Boolean = {
    val saved = load()
    val alreadyContains = contains(item, saved)
    if (!alreadyContains) {
      persist(saved :+ item)
    }
    !alreadyContains
  }

  def contains(elem: T, others: Seq[T]): Boolean = others contains elem
}