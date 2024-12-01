package com.malliina.audio.javasound

import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize
import org.apache.pekko.stream.Materializer

import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

class BasicJavaSoundPlayer(
  media: StreamSource,
  readWriteBufferSize: StorageSize = DefaultRwBufferSize
)(implicit mat: Materializer)
  extends JavaSoundPlayer(media.toOneShot, readWriteBufferSize)
  with SourceClosing:

  override def resetStream(oldStream: InputStream): InputStream =
    oldStream.close()
    media.openStream

  override def seekProblem: Option[String] = None

object BasicJavaSoundPlayer:
  def fromFile(file: Path, mat: Materializer) =
    new BasicJavaSoundPlayer(StreamSource.fromFile(file))(mat)

  def fromUri(uri: URI, duration: FiniteDuration, size: StorageSize, mat: Materializer) =
    new BasicJavaSoundPlayer(StreamSource.fromURI(uri, duration, size))(mat)
