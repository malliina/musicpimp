package com.malliina.musicpimp.audio

import cats.effect.Async
import cats.effect.std.Dispatcher
import com.malliina.audio.javasound.{BasicJavaSoundPlayer, JavaSoundPlayer}
import com.malliina.audio.meta.OneShotStream
import com.malliina.audio.{PlaybackEvents, PlayerStates}
import com.malliina.musicpimp.library.LocalTrack
import fs2.concurrent.Topic

class StoragePlayer[F[_]: Async](
  val track: LocalTrack,
  states: Topic[F, PlayerStates],
  timeUpdatesTopic: Topic[F, PlaybackEvents.TimeUpdated],
  d: Dispatcher[F],
  eom: () => F[Unit]
) extends BasicJavaSoundPlayer[F](track.media, states, timeUpdatesTopic, d)
  with PimpPlayer[F]:
  override def onEndOfMedia(): F[Unit] = eom()

class StreamPlayer[F[_]: Async](
  val track: StreamedTrack,
  states: Topic[F, PlayerStates],
  timeUpdatesTopic: Topic[F, PlaybackEvents.TimeUpdated],
  d: Dispatcher[F],
  eom: () => F[Unit]
) extends JavaSoundPlayer[F](
    OneShotStream(track.stream, track.duration, track.size),
    states,
    timeUpdatesTopic,
    d,
    JavaSoundPlayer.DefaultRwBufferSize
  )
  with PimpPlayer[F]:
  override def onEndOfMedia(): F[Unit] = eom()
