package com.malliina.musicpimp.library

import com.malliina.file.FileUtilities
import com.malliina.musicpimp.library.Settings.log
import com.malliina.musicpimp.util.FileUtil
import com.malliina.util.AppLogger
import io.circe.parser.parse
import io.circe.syntax.EncoderOps

import java.nio.file.{Files, Path, Paths}

object Settings extends Settings:
  private val log = AppLogger(getClass)

trait Settings:
  private val settingsFile = FileUtil.localPath("settings.json")
  private val FOLDERS = "folders"

  def readFolders: Seq[String] = read.map(_.toString)

  def read: Seq[Path] =
    if Files.exists(settingsFile) then
      val jsonString = FileUtilities.readerFrom(settingsFile)(_.mkString(FileUtilities.lineSep))
      log.debug(s"Reading: $jsonString")
      val pathStrings =
        parse(jsonString).flatMap(_.hcursor.downField(FOLDERS).as[Seq[String]]).getOrElse(Nil)
      pathStrings.map(Paths.get(_))
    else Nil

  def save(folders: Seq[Path]): Unit =
    val pathStrings = folders.map(_.toAbsolutePath.toString)
    val json = Map(FOLDERS -> pathStrings).asJson
    val jsonString = json.noSpaces
    log.debug(s"Saving: $jsonString")
    FileUtilities.writerTo(settingsFile)(_.println(jsonString))

  def add(folder: Path): Unit = save(folder +: read)

  def delete(folder: Path): Unit = save(read.filter(_ != folder))
