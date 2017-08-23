package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.audio.javasound.JavaSoundPlayer

trait PimpPlayer extends JavaSoundPlayer {
  def playState = PimpPlayer.playState(state)

  def track: PlayableTrack
}

object PimpPlayer {
  def playState(s: PlayerStates.PlayerState): PlayState = s match {
    case PlayerStates.Unrealized => Unrealized
    case PlayerStates.Realizing => Realizing
    case PlayerStates.Realized => Realized
    case PlayerStates.Prefetching => Prefetching
    case PlayerStates.Prefetched => Prefetched
    case PlayerStates.NoMedia => NoMedia
    case PlayerStates.Open => Open
    case PlayerStates.Started => Started
    case PlayerStates.Stopped => Stopped
    case PlayerStates.Closed => Closed
    case PlayerStates.EndOfMedia => EndOfMedia
    case _ => Unknown
  }
}
