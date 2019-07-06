package com.malliina.musicpimp.util

import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{FileAlreadyExistsException, Files, Path, Paths}

import com.malliina.file.{FileUtilities, StorageFile}
import com.malliina.util.{EnvUtils, Utils}

object FileUtil {
  val ownerOnlyPermissions = PosixFilePermissions fromString "rw-------"
  val ownerOnlyAttributes = PosixFilePermissions asFileAttribute ownerOnlyPermissions

  val pimpHomeDir = appHome orElse localDirWindows getOrElse localDirDefault

  def localPath(name: String) = pimpHomeDir / name

  protected def appHome: Option[Path] = findPath(sys.props.get("musicpimp.home"))

  protected def localDirWindows: Option[Path] =
    findPath(
      sys.env
        .get("ALLUSERSPROFILE")
        .orElse(sys.env.get("LOCALAPPDATA")))
      .map(_ / "MusicPimp")
      .filterNot(_ => EnvUtils.operatingSystem.isUnixLike)

  protected def localDirDefault: Path = FileUtilities.tempDir / ".musicpimp"

  private def findPath(dir: Option[String]): Option[Path] = dir.map(dir => Paths.get(dir))

  def pathTo(file: String, createIfNotExists: Boolean = false): Path = {
    val path = localPath(file)
    if (!Files.exists(path)) {
      Utils.opt[Unit, FileAlreadyExistsException](Files.createFile(path))
    }
    path
  }

  // TODO DRY, this is in util
  def props(file: Path): Map[String, String] = {
    if (Files.exists(file)) {
      val src = scala.io.Source.fromFile(file.toFile)
      try {
        val kvs = src
          .getLines()
          .flatMap(line => {
            val kv = line.split("=", 2)
            if (kv.size >= 2) {
              Some(kv(0) -> kv(1))
            } else {
              None
            }
          })
        Map(kvs.toList: _*)
      } finally {
        src.close()
      }
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
      Files setPosixFilePermissions (file, perms)
    }

  def trySetOwnerOnlyPermissions(file: Path) = trySetPermissions(file, ownerOnlyPermissions)
}
