package com.malliina.musicpimp.models

import java.nio.file.Path

import com.malliina.play.json.SimpleCompanion
import play.api.data.format.Formats.stringFormat
import slick.jdbc.H2Profile.api.{MappedColumnType, stringColumnType}

/**
  * @param path a path - all separators must be slashes ('/') regardless of platform
  */
case class PimpPath(path: String) extends BasePath {
  override def toString: String = path
}

object PimpPath extends SimpleCompanion[String, PimpPath] {
  val UnixPathSeparator: Char = '/'
  val WindowsPathSeparator = '\\'
  val Empty = PimpPath("")
  implicit val db = MappedColumnType.base[PimpPath, String](raw, fromRaw)

  override def raw(t: PimpPath): String = t.path

  def apply(path: Path): PimpPath = fromRaw(path.toString)

  def fromRaw(s: String): PimpPath = PimpPath(s.replace(WindowsPathSeparator, UnixPathSeparator))
}
