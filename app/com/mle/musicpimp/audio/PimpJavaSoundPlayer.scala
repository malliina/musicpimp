package com.mle.musicpimp.audio

import com.mle.musicpimp.library.TrackInfo
import com.mle.audio.javasound.JavaSoundPlayer

/**
 *
 * @author mle
 */
class PimpJavaSoundPlayer(val track: TrackInfo)
  extends JavaSoundPlayer(track.meta.media)
  with PimpPlayer {
  val meta = track.meta
}