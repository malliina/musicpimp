package com.malliina.musicpimp.audio

import com.malliina.audio.javasound.JavaSoundPlayer

trait PimpPlayer extends JavaSoundPlayer {
  def track: PlayableTrack
}