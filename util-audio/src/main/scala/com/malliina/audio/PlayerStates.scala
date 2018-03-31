package com.malliina.audio

object PlayerStates extends Enumeration {
  type PlayerState = Value
  val Unrealized, Realizing, Realized,
  Prefetching, Prefetched, NoMedia,
  Open, Started, Stopped,
  Closed, Unknown, EndOfMedia = Value

  def fromInt(state: Int) = state match {
    case 100 => Unrealized
    case 200 => Realizing
    case 300 => Realized
    case 400 => Prefetching
    case 500 => Prefetched
    case 600 => Started
    case _ => throw new IllegalArgumentException("Unknown player state identifier: " + state)
  }
}
