package com.malliina.musicpimp.audio

import com.malliina.audio.javasound.JavaSoundPlayer


/**
  * @author Michael
  */
trait PimpPlayer extends JavaSoundPlayer {
  def track: PlayableTrack
}