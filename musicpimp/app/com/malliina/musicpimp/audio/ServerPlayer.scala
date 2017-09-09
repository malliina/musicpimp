package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import rx.lang.scala.Observable

import scala.concurrent.duration.Duration

trait ServerPlayer {
  def allEvents: Observable[ServerMessage]

  def position: Duration

  def status(host: FullUrl): StatusEvent

  def status17(host: FullUrl): StatusEvent17
}
