package com.malliina.audio.meta

import java.nio.file.{Files, Path}

import org.jaudiotagger.audio.AudioFileIO

import scala.concurrent.duration.DurationDouble

case class SongMeta(media: StreamSource, tags: SongTags)

object SongMeta {
  def fromPath(path: Path): SongMeta = fromPath(path, Option(path.getRoot).getOrElse(path))

  def fromPath(absolutePath: Path, root: Path): SongMeta = {
    val audioFile = AudioFileIO read absolutePath.toFile
    val duration = audioFile.getAudioHeader.getTrackLength.toDouble.seconds
    val tags = SongTags.fromAudioFile(audioFile).getOrElse(SongTags.fromFilePath(absolutePath, root))
    SongMeta(FileSource(absolutePath, duration), tags)
  }

  def fromFilePath(path: Path, root: Path) = {
    val relativePath = root relativize path
    val maybeParent = Option(relativePath.getParent)
    val title = titleFromFileName(path)
    val album = maybeParent.flatMap(p => Option(p.getFileName).map(_.toString)).getOrElse("")
    // Both getParent and getFileName may return null. Thanks, Java.
    val artist = maybeParent.map(p => {
      Option(p.getParent).map(pp => {
        Option(pp.getFileName).map(_.toString).getOrElse(album)
      }).getOrElse(album)
    }).getOrElse(album)
    SongMeta(StreamSource.fromFile(path), SongTags(title, album, artist))
  }

  def titleFromFileName(path: Path) = {
    val fileName = Option(path.getFileName).map(_.toString).getOrElse("")
    if (fileName endsWith ".mp3") fileName.slice(0, fileName.length - 4) else fileName
  }

  def titleOf(absolutePath: Path) = {
    if (Files isDirectory absolutePath) {
      titleFromFileName(absolutePath)
    } else {
      SongTags.fromTags(absolutePath).map(_.title).filter(_.nonEmpty)
        .getOrElse(titleFromFileName(absolutePath))
    }
  }
}
