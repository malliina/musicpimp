package com.malliina.musicpimp.audio

import cats.effect.Async
import cats.effect.std.Dispatcher
import com.malliina.audio.{PlaybackEvents, PlayerStates}
import com.malliina.musicpimp.models.TrackID
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath
import fs2.concurrent.Topic

import java.io.InputStream
import scala.concurrent.duration.FiniteDuration

case class StreamedTrack(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  path: UnixPath,
  duration: FiniteDuration,
  size: StorageSize,
  stream: InputStream
) extends PlayableTrack:
  override def buildPlayer[F[_]: Async](
    states: Topic[F, PlayerStates],
    timeUpdates: Topic[F, PlaybackEvents.TimeUpdated],
    d: Dispatcher[F],
    eom: () => F[Unit]
  ): PimpPlayer[F] =
    StreamPlayer(this, states, timeUpdates, d, eom)

object StreamedTrack:
  def fromTrack(t: TrackMeta, inStream: InputStream): StreamedTrack =
    StreamedTrack(t.id, t.title, t.artist, t.album, t.path, t.duration, t.size, inStream)
