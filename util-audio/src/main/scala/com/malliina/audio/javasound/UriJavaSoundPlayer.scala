package com.malliina.audio.javasound

import java.net.URI

import org.apache.pekko.stream.Materializer
import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize

import scala.concurrent.duration.FiniteDuration

class UriJavaSoundPlayer(
  uri: URI,
  duration: FiniteDuration,
  size: StorageSize,
  readWriteBufferSize: StorageSize = DefaultRwBufferSize
)(implicit mat: Materializer)
  extends BasicJavaSoundPlayer(StreamSource.fromURI(uri, duration, size), readWriteBufferSize)
  with SourceClosing
