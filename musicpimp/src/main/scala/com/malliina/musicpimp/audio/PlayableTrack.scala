package com.malliina.musicpimp.audio

import cats.effect.Async
import cats.effect.std.Dispatcher
import com.malliina.audio.{PlaybackEvents, PlayerStates}
import fs2.concurrent.Topic

trait PlayableTrack extends TrackMeta:
  def buildPlayer[F[_]: Async](
    states: Topic[F, PlayerStates.PlayerState],
    timeUpdates: Topic[F, PlaybackEvents.TimeUpdated],
    d: Dispatcher[F],
    eom: () => F[Unit]
  ): PimpPlayer[F]
