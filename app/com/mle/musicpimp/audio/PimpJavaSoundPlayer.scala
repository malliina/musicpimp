package com.mle.musicpimp.audio

import com.mle.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.mle.musicpimp.library.LocalTrack

class StoragePlayer(val track: LocalTrack)
  extends BasicJavaSoundPlayer(track.media)
  with PimpPlayer

class StreamPlayer(val track: StreamedTrack)
  extends JavaSoundPlayer(track.stream, track.duration, track.size)
  with PimpPlayer