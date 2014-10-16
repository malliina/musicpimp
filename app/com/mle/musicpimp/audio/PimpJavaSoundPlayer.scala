package com.mle.musicpimp.audio

import com.mle.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.mle.musicpimp.library.LocalTrack

class StoragePlayer(val track: LocalTrack, eom: () => Unit)
  extends BasicJavaSoundPlayer(track.media)
  with PimpPlayer {
  override def onEndOfMedia(): Unit = eom()
}

class StreamPlayer(val track: StreamedTrack, eom: () => Unit)
  extends JavaSoundPlayer(track.stream, track.duration, track.size, JavaSoundPlayer.DEFAULT_RW_BUFFER_SIZE)
  with PimpPlayer {
  override def onEndOfMedia(): Unit = eom()
}