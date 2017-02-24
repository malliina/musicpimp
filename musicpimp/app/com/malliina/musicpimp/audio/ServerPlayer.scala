package com.malliina.musicpimp.audio

import rx.lang.scala.Observable

import scala.concurrent.duration.Duration

trait ServerPlayer {
  def allEvents: Observable[ServerMessage]

  def position: Duration

  def status: StatusEvent

  def status17: StatusEvent17
}
