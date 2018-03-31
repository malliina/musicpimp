package com.malliina.audio

import scala.concurrent.duration.Duration

object PlaybackEvents {

  trait PlaybackEvent

  case object EndOfMedia extends PlaybackEvent

  case class TimeUpdated(position: Duration) extends PlaybackEvent

  case object Started extends PlaybackEvent

  case object Stopped extends PlaybackEvent

  case object Closed extends PlaybackEvent
}
