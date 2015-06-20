package com.mle.musicpimp.util

import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{Paths, FileAlreadyExistsException, Files, Path}

import com.mle.file.{FileUtilities, StorageFile}
import com.mle.util.{EnvUtils, Util, Log, Utils}

/**
 * @author Michael
 */
object FileUtil {
  val ownerOnlyPermissions = PosixFilePermissions fromString "rw-------"
  val ownerOnlyAttributes = PosixFilePermissions asFileAttribute ownerOnlyPermissions

  val pimpHomeDir = appHome orElse localDirWindows getOrElse localDirDefault

  def localPath(name: String) = pimpHomeDir / name

  protected def appHome = findPath(sys.props.get("musicpimp.home"))

  protected def localDirWindows = findPath(sys.env.get("LOCALAPPDATA")).map(_ / "MusicPimp")
    .filterNot(_ => EnvUtils.operatingSystem.isUnixLike)

  protected def localDirDefault = FileUtilities.tempDir / ".musicpimp"

  private def findPath(dir: Option[String]) = dir.map(dir => Paths.get(dir))

  def pathTo(file: String, createIfNotExists: Boolean = false): Path = {
    val path = localPath(file)
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
