package com.malliina.musicpimp.audio

import com.malliina.audio.PlaylistState
import com.malliina.http.FullUrl

import scala.concurrent.duration.Duration

trait ServerPlayer[F[_]]:
  def allEvents: fs2.Stream[F, ServerMessage]
  def position: Duration
  def status(host: FullUrl, playlist: PlaylistState[PlayableTrack]): StatusEvent
  def status17(host: FullUrl, playlist: PlaylistState[PlayableTrack]): StatusEvent17
