package com.malliina.musicpimp.library

import java.nio.file.{Files, Path, Paths}

import com.malliina.file.FileUtilities
import com.malliina.musicpimp.library.Settings.log
import com.malliina.musicpimp.util.FileUtil
import play.api.Logger
import play.api.libs.json.Json

trait Settings:
  val settingsFile = FileUtil.localPath("settings.json")
  val FOLDERS = "folders"

  def readFolders: Seq[String] = read.map(_.toString)

  def read: Seq[Path] =
    if Files.exists(settingsFile) then
      val jsonString = FileUtilities.readerFrom(settingsFile)(_.mkString(FileUtilities.lineSep))
      val json = Json parse jsonString
      log debug s"Reading: $jsonString"
      val pathStrings = (json \ FOLDERS).as[Seq[String]]
      pathStrings map (Paths.get(_))
    else Nil

  def save(folders: Seq[Path]): Unit =
    val pathStrings = folders map (_.toAbsolutePath.toString)
    val json = Json toJson Map(FOLDERS -> pathStrings)
    val jsonString = Json stringify json
    log debug s"Saving: $jsonString"
    FileUtilities.writerTo(settingsFile)(_.println(jsonString))

  def add(folder: Path): Unit = save(folder +: read)

  def delete(folder: Path): Unit = save(read.filter(_ != folder))

object Settings extends Settings:
  private val log = Logger(getClass)
