package com.malliina.musicpimp.audio

import com.malliina.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.malliina.musicpimp.library.LocalTrack
import org.apache.pekko.stream.Materializer

class StoragePlayer(val track: LocalTrack, eom: () => Unit)(implicit mat: Materializer)
  extends BasicJavaSoundPlayer(track.media)
  with PimpPlayer:
  override def onEndOfMedia(): Unit = eom()

class StreamPlayer(val track: StreamedTrack, eom: () => Unit)(implicit mat: Materializer)
  extends JavaSoundPlayer(
    track.stream,
    track.duration,
    track.size,
    JavaSoundPlayer.DefaultRwBufferSize
  )
  with PimpPlayer:
  override def onEndOfMedia(): Unit = eom()
