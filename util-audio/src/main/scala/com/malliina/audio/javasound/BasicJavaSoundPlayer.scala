package com.malliina.audio.javasound

import cats.effect.kernel.Async
import cats.effect.std.Dispatcher
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.audio.{PlaybackEvents, PlayerStates}
import com.malliina.audio.javasound.JavaSoundPlayer.DefaultRwBufferSize
import com.malliina.audio.meta.StreamSource
import com.malliina.storage.StorageSize
import fs2.concurrent.Topic

import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import scala.concurrent.duration.FiniteDuration

class BasicJavaSoundPlayer[F[_]: Async](
  media: StreamSource,
  states: Topic[F, PlayerStates.PlayerState],
  timeUpdatesTopic: Topic[F, PlaybackEvents.TimeUpdated],
  d: Dispatcher[F],
  readWriteBufferSize: StorageSize = DefaultRwBufferSize
) extends JavaSoundPlayer[F](media.toOneShot, states, timeUpdatesTopic, d, readWriteBufferSize)
  with SourceClosing[F]:

  override def resetStream(oldStream: InputStream): InputStream =
    oldStream.close()
    media.openStream

  override def seekProblem: Option[String] = None

object BasicJavaSoundPlayer:
  def default[F[_]: Async](
    media: StreamSource,
    d: Dispatcher[F],
    readWriteBufferSize: StorageSize = DefaultRwBufferSize
  ): F[BasicJavaSoundPlayer[F]] =
    for
      states <- Topic[F, PlayerStates.PlayerState]
      timeUpdates <- Topic[F, PlaybackEvents.TimeUpdated]
    yield BasicJavaSoundPlayer(media, states, timeUpdates, d, readWriteBufferSize)

  def fromFile[F[_]: Async](
    file: Path,
    d: Dispatcher[F],
    readWriteBufferSize: StorageSize = DefaultRwBufferSize
  ): F[BasicJavaSoundPlayer[F]] =
    default[F](StreamSource.fromFile(file), d, readWriteBufferSize)

  def fromUri[F[_]: Async](
    uri: URI,
    duration: FiniteDuration,
    size: StorageSize,
    d: Dispatcher[F]
  ): F[BasicJavaSoundPlayer[F]] =
    default[F](StreamSource.fromURI(uri, duration, size), d)
