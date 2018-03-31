package com.malliina.audio.javasound

import java.io.InputStream
import java.net.URI
import java.nio.file.Path

import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize

import scala.concurrent.duration.Duration

class BasicJavaSoundPlayer(media: StreamSource, readWriteBufferSize: StorageSize = DefaultRwBufferSize)
  extends JavaSoundPlayer(media.toOneShot, readWriteBufferSize)
    with SourceClosing {

  override def resetStream(oldStream: InputStream): InputStream = {
    oldStream.close()
    media.openStream
  }

  override def seekProblem: Option[String] = None
}

object BasicJavaSoundPlayer {
  def fromFile(file: Path) =
    new BasicJavaSoundPlayer(StreamSource.fromFile(file))

  def fromUri(uri: URI, duration: Duration, size: StorageSize) =
    new BasicJavaSoundPlayer(StreamSource.fromURI(uri, duration, size))
}
