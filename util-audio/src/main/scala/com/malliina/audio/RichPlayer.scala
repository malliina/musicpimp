package com.malliina.audio

import scala.concurrent.duration.Duration

trait RichPlayer[F[_]] extends IPlayer[F]:
  def duration: Duration
  def position: Duration
  def volume: Int
  def mute: Boolean
