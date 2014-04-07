package com.mle.musicpimp.audio

import com.mle.audio.javasound.JavaSoundPlayer


/**
 * @author Michael
 */
trait PimpPlayer extends JavaSoundPlayer {
  def track: PlayableTrack
}