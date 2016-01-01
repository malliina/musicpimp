package com.malliina.musicpimp.util

import java.nio.file.Path

import com.malliina.file.FileUtilities
import play.api.libs.json.{Reads, Writes}

import scala.util.Try

/**
  * @author Michael
  */
trait JsonStore {
  def save[T](t: T, file: Path)(implicit writer: Writes[T])

  def load[T](file: Path)(implicit reader: Reads[T]): Try[T]
}

object JsonStorage extends JsonStore {

  import play.api.libs.json.Json.{parse, stringify, toJson}

  override def save[T](t: T, file: Path)(implicit writer: Writes[T]): Unit =
    FileUtilities.stringToFile(stringify(toJson(t)), file)

  override def load[T](file: Path)(implicit reader: Reads[T]): Try[T] =
    Try(parse(FileUtilities.fileToString(file)).as[T])

  def load[T](file: Path, default: T)(implicit reader: Reads[T]): T = load(file) getOrElse default
}
