package com.malliina.musicpimp.library

import cats.effect.Async
import cats.effect.std.Dispatcher
import com.malliina.audio.{PlaybackEvents, PlayerStates}
import com.malliina.audio.meta.{SongMeta, StreamSource}
import com.malliina.musicpimp.audio.{PimpPlayer, PlayableTrack, StoragePlayer}
import com.malliina.musicpimp.library.LocalTrack.log
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.util.AppLogger
import com.malliina.values.UnixPath
import fs2.concurrent.Topic

import scala.concurrent.duration.*

object LocalTrack:
  private val log = AppLogger(getClass)

class LocalTrack(val id: TrackID, val path: UnixPath, val meta: SongMeta)
  extends MusicItem
  with PlayableTrack:
  val media: StreamSource = meta.media
  override val title = meta.tags.title
  override val size: StorageSize = media.size
  override val duration: FiniteDuration = media.duration
  override val album: String = meta.tags.album
  override val artist: String = meta.tags.artist

  override def toString = id.id

  override def buildPlayer[F[_]: Async](
    states: Topic[F, PlayerStates.PlayerState],
    timeUpdates: Topic[F, PlaybackEvents.TimeUpdated],
    d: Dispatcher[F],
    eom: () => F[Unit]
  ): PimpPlayer[F] =
    log.info(s"Preparing local track '$title' by '$artist' using ${media.describe}...")
    StoragePlayer(this, states, timeUpdates, d, eom)
