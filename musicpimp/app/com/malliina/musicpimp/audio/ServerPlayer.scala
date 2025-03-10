package com.malliina.musicpimp.audio

import org.apache.pekko.NotUsed
import org.apache.pekko.stream.scaladsl.Source
import com.malliina.http.FullUrl

import scala.concurrent.duration.Duration

trait ServerPlayer:
  def allEvents: Source[ServerMessage, NotUsed]
  def position: Duration
  def status(host: FullUrl): StatusEvent
  def status17(host: FullUrl): StatusEvent17
