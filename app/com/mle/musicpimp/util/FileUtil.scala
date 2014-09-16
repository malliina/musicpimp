package com.mle.musicpimp.util

import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{FileAlreadyExistsException, Files, Path}

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.util.{Log, Utils}

/**
 * @author Michael
 */
object FileUtil extends Log {
  val ownerOnlyPermissions = PosixFilePermissions fromString "rw-------"
  val ownerOnlyAttributes = PosixFilePermissions asFileAttribute ownerOnlyPermissions
  val pimpHomeDir = FileUtilities.userDir / ".musicpimp"

  def pathTo(file: String, createIfNotExists: Boolean = false): Path = {
    val path = pimpHomeDir / file
    if (!Files.exists(path)) {
      Utils.opt[Unit, FileAlreadyExistsException](Files.createFile(path))
    }
    path
  }

  // TODO DRY, this is in util
  def props(file: Path) = {
    if (Files.exists(file)) {
      val kvs = scala.io.Source.fromFile(file.toFile).getLines().flatMap(line => {
        val kv = line.split("=", 2)
        if (kv.size >= 2) {
          Some(kv(0) -> kv(1))
        } else {
          None
        }
      })
      Map(kvs.toList: _*)
    } else {
      Map.empty[String, String]
    }
  }

  def firstLine(path: Path): Option[String] =
    if (Files exists path) {
      FileUtilities.readerFrom(path)(_.find(_ => true))
    } else {
      None
    }

  def trySetPermissions(file: Path, perms: java.util.Set[PosixFilePermission]) =
    Utils.opt[Unit, UnsupportedOperationException] {
      Files setPosixFilePermissions(file, perms)
    }

  def trySetOwnerOnlyPermissions(file: Path) = trySetPermissions(file, ownerOnlyPermissions)
}
