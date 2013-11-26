package com.mle.musicpimp.audio

import com.mle.audio.javasound.JavaSoundPlayer
import com.mle.musicpimp.library.TrackInfo

/**
 *
 * @author mle
 */
abstract class PimpJavaSoundPlayer(val track: TrackInfo)
  extends JavaSoundPlayer(track.meta.media)
  with PimpPlayer {
  val meta = track.meta
}