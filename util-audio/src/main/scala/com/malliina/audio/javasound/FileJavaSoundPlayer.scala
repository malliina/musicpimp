package com.malliina.audio.javasound

import java.nio.file.Path

import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize

/** Use for audio files. Since this constructor opens an InputStream, trait SourceClosing is mixed in so that when this
  * player is closed, so is the InputStream.
  *
  * @param file file to play
  */
class FileJavaSoundPlayer(file: Path, readWriteBufferSize: StorageSize = DefaultRwBufferSize)
  extends BasicJavaSoundPlayer(StreamSource.fromFile(file), readWriteBufferSize)
