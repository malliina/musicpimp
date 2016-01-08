package com.malliina.util

/**
  * @author mle
  */
object Lists {
  def insertAt[T](pos: Int, xs: Seq[T], elem: T) = {
    val (left, right) = xs.splitAt(pos)
    left ++ Seq(elem) ++ right
  }

  /** Removes the element at `pos` from `xs`.
    *
    * @param pos index
    * @param xs elements
    * @return a new `Seq` with the element at `pos` removed
    */
  def removeAt[T](pos: Int, xs: Seq[T]): Seq[T] = {
    val (left, right) = xs.splitAt(pos)
    left ++ right.drop(1)
  }

  /**
    *
    * @param source source index
    * @param dest destination index
    * @param xs source list
    * @return a new list where the element that used to be at `source` is at `dest`
    */
  def move[T](source: Int, dest: Int, xs: Seq[T]): Seq[T] = {
    val mover = xs(source)
    insertAt(dest, removeAt(source, xs), mover)
  }
}
