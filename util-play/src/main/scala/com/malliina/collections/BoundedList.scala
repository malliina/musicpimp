package com.malliina.collections

import scala.collection.mutable.ListBuffer

/** A mutable list with a bounded maximum size.
  *
  * Not thread-safe, but designed to be used within an `Actor`,
  * in which case thread-safety is not needed.
  *
  * @param bufferSize size
  * @tparam T type of element
  */
class BoundedList[T](bufferSize: Int) {
  private val inner = ListBuffer[T]()

  def +=(t: T) = append(t)

  def append(t: T) = {
    inner append t
    val overflow = inner.size - bufferSize
    if (overflow > 0) {
      inner.dropInPlace(overflow)
    }
  }

  def foreach[U](f: T => U): Unit = inner foreach f

  def toList: List[T] = inner.toList

  def size: Int = inner.size

  def isEmpty: Boolean = inner.isEmpty
}

object BoundedList {
  def empty[T](bufferSize: Int) = new BoundedList[T](bufferSize)
}
