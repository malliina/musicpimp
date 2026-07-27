package com.malliina.io

import java.nio.file.{Files, Path}
import com.malliina.file.FileUtilities
import io.circe.{Codec, Json}
import io.circe.syntax.EncoderOps

/** Persists the list to `file` by serializing at every modification.
  *
  * @param file
  *   backing storage
  * @param jsonFormat
  *   serialization instructions
  * @tparam T
  *   type of element
  */
class FileBackedList[T](file: Path)(implicit val jsonFormat: Codec[T]) extends PersistentList[T]:
  override protected def persist(items: Seq[T]): Unit =
    this.synchronized(FileUtilities.stringToFile(items.asJson.noSpaces, file))

  override protected def load(): Seq[T] =
    if Files.isReadable(file) then
      io.circe.parser
        .decode[Vector[T]](this.synchronized(FileUtilities.fileToString(file)))
        .getOrElse(Vector.empty)
    else Vector.empty

class FileBackedSet[T](file: Path)(implicit jsonFormat: Codec[T])
  extends FileBackedList[T](file)
  with Distinctness[T]
