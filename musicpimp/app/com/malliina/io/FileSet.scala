package com.malliina.io

import com.malliina.file.FileUtilities
import play.api.libs.json.Format

abstract class FileSet[T](file: String)(implicit format: Format[T])
  extends FileBackedSet[T](FileUtilities pathTo file) with LoggingList[T] {
  protected def id(elem: T): String

  def areSame(first: T, second: T) = id(first) == id(second)

  override def contains(elem: T, others: Seq[T]): Boolean = others.exists(areSame(elem, _))

  override def filterNot(elem: T, others: Seq[T]): Seq[T] = others.filterNot(areSame(elem, _))

  def withID(elemID: String): Option[T] = get().find(e => id(e) == elemID)

  def removeID(id: String) = withID(id).foreach(remove)
}
