package com.malliina.musicpimp.util

import com.malliina.file.{FileUtilities, StorageFile}
import com.malliina.util.EnvUtils

import java.nio.file.attribute.{PosixFilePermission, PosixFilePermissions}
import java.nio.file.{Files, Path, Paths}
import scala.util.Try

object FileUtil:
  val userHome = Paths.get(sys.props("user.home"))
  val ownerOnlyPermissions = PosixFilePermissions.fromString("rw-------")
  val ownerOnlyAttributes = PosixFilePermissions.asFileAttribute(ownerOnlyPermissions)

  val pimpHomeDir = appHome orElse localDirWindows getOrElse localDirDefault

  def localPath(name: String) = pimpHomeDir / name

  protected def appHome: Option[Path] = findPath(sys.props.get("musicpimp.home"))

  protected def localDirWindows: Option[Path] =
    findPath(
      sys.env
        .get("ALLUSERSPROFILE")
        .orElse(sys.env.get("LOCALAPPDATA"))
    ).map(_ / "MusicPimp")
      .filterNot(_ => EnvUtils.operatingSystem.isUnixLike)

  protected def localDirDefault: Path = userHome.resolve(".musicpimp")

  private def findPath(dir: Option[String]): Option[Path] = dir.map(dir => Paths.get(dir))

  def pathTo(file: String, createIfNotExists: Boolean = false): Path =
    val path = localPath(file)
    if !Files.exists(path) then Try(Files.createFile(path))
    path

  // TODO DRY, this is in util
  def props(file: Path): Map[String, String] =
    if Files.exists(file) then
      val src = scala.io.Source.fromFile(file.toFile)
      try
        val kvs = src
          .getLines()
          .flatMap: line =>
            val kv = line.split("=", 2)
            if kv.size >= 2 then Some(kv(0) -> kv(1))
            else None
        Map(kvs.toList*)
      finally src.close()
    else Map.empty[String, String]

  def firstLine(path: Path): Option[String] =
    if Files.exists(path) then FileUtilities.readerFrom(path)(_.find(_ => true))
    else None

  def trySetPermissions(file: Path, perms: java.util.Set[PosixFilePermission]) =
    try Option(Files.setPosixFilePermissions(file, perms))
    catch case e: UnsupportedOperationException => None

  def trySetOwnerOnlyPermissions(file: Path) = trySetPermissions(file, ownerOnlyPermissions)
