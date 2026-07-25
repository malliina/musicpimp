package com.malliina.audio

import scala.concurrent.duration.FiniteDuration

trait RichPlayer[F[_]] extends IPlayer[F]:
  def duration: FiniteDuration
  def position: FiniteDuration
  def volume: Int
  def mute: Boolean
