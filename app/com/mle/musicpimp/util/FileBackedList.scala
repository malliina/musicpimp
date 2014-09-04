package com.mle.musicpimp.util

import java.nio.file.{Files, Path}

import com.mle.util.FileUtilities
import play.api.libs.json.Format
import play.api.libs.json.Json._

/**
 * Persists the list to `file` by serializing at every modification. Not thread-safe.
 *
 * @param file backing storage
 * @param jsonFormat knowledge of serializing the elements
 * @tparam T type of element
 */
class FileBackedList[T](file: Path)(implicit val jsonFormat: Format[T]) extends PersistentList[T] {
  override protected def persist(items: Seq[T]): Unit =
    FileUtilities.stringToFile(stringify(toJson(items)), file)

  override protected def load(): Seq[T] =
    if (Files.isReadable(file)) {
      parse(FileUtilities.fileToString(file))
        .asOpt[Vector[T]]
        .getOrElse(Vector.empty)
    } else {
      Vector.empty
    }
}

class FileBackedSet[T](file: Path)(implicit jsonFormat: Format[T])
  extends FileBackedList[T](file)
  with Distinctness[T]