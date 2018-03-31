package com.malliina.audio.meta

import java.io.{BufferedInputStream, FileInputStream, InputStream}
import java.net.URI
import java.nio.file.{Files, Path}

import com.malliina.storage.{StorageLong, StorageSize}

import scala.concurrent.duration.Duration

trait MediaMeta {
  def duration: Duration

  def size: StorageSize
}

trait StreamSource extends MediaMeta {
  def openStream: InputStream

  def toOneShot = OneShotStream(openStream, duration, size)
}

object StreamSource {
  def fromFile(file: Path) =
    FileSource(file, MediaTags.audioDuration(file))

  def fromURI(uri: URI, duration: Duration, size: StorageSize) =
    UriSource(uri, duration, size)
}

case class OneShotStream(stream: InputStream, duration: Duration, size: StorageSize) extends MediaMeta

case class UriSource(uri: URI, duration: Duration, size: StorageSize) extends StreamSource {
  override def openStream: InputStream = uri.toURL.openStream()
}

case class FileSource(file: Path, duration: Duration) extends StreamSource {
  override val size: StorageSize = (Files size file).bytes

  def openStream: InputStream = new BufferedInputStream(new FileInputStream(file.toFile))
}
