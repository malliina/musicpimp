package com.malliina.audio.javasound

import java.nio.file.Path

import org.apache.pekko.stream.Materializer
import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize

/** Use for audio files. Since this constructor opens an InputStream, trait SourceClosing is mixed
  * in so that when this player is closed, so is the InputStream.
  *
  * @param file
  *   file to play
  */
class FileJavaSoundPlayer(file: Path, readWriteBufferSize: StorageSize = DefaultRwBufferSize)(
  implicit mat: Materializer
) extends BasicJavaSoundPlayer(StreamSource.fromFile(file), readWriteBufferSize)
