package com.malliina.musicpimp.audio

import com.malliina.musicpimp.models.Volume

import scala.concurrent.duration.Duration

object StatusEvents:
  val empty = StatusEvent(
    FullTrack.empty,
    PlayState.closed,
    position = Duration.fromNanos(0),
    volume = Volume(40),
    mute = false,
    playlist = Seq.empty,
    index = -1
  )
