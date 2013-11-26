package com.mle.musicpimp.audio

/**
 * @author Michael
 */
@deprecated("Use the PlayerStates Enumeration instead", "1.7.0")
object PlayState extends Enumeration {
  type PlayState = Value
  val Playing, Stopped, Paused, NotPlaying, NoMedia = Value
}