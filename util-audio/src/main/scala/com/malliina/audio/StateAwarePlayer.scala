package com.malliina.audio

trait StateAwarePlayer extends IPlayer {
  def state: PlayerStates.PlayerState

  def onEndOfMedia(): Unit = ()
}
