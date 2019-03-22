package com.malliina.io

import java.nio.file.{Files, Path}

import com.malliina.file.FileUtilities
import play.api.libs.json.Format
import play.api.libs.json.Json._

/**
 * Persists the list to `file` by serializing at every modification.
 *
 * @param file backing storage
 * @param jsonFormat serialization instructions
 * @tparam T type of element
 */
class FileBackedList[T](file: Path)(implicit val jsonFormat: Format[T]) extends PersistentList[T] {
  override protected def persist(items: Seq[T]): Unit = {
    this.synchronized(FileUtilities.stringToFile(stringify(toJson(items)), file))
  }

  override protected def load(): Seq[T] =
    if (Files.isReadable(file)) {
      parse(this.synchronized(FileUtilities.fileToString(file)))
        .asOpt[Vector[T]]
        .getOrElse(Vector.empty)
    } else {
      Vector.empty
    }
}

class FileBackedSet[T](file: Path)(implicit jsonFormat: Format[T])
  extends FileBackedList[T](file)
    with Distinctness[T]
