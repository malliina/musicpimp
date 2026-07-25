package com.malliina.musicpimp.audio

import cats.effect.Resource
import cats.effect.std.Dispatcher
import cats.effect.{Async, Concurrent, Ref}
import cats.implicits.{catsSyntaxApplicativeError, catsSyntaxFlatMapOps}
import cats.syntax.all.{toFlatMapOps, toFunctorOps}
import com.malliina.audio.*
import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.MusicPlayer.log
import com.malliina.musicpimp.models.Volume
import com.malliina.util.AppLogger
import fs2.concurrent.Topic
import fs2.Stream

import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import javax.sound.sampled.LineUnavailableException
import scala.concurrent.duration.{Duration, DurationInt, FiniteDuration}

object MusicPlayer:
  private val log = AppLogger(getClass)

  def default[F[_]: Async](d: Dispatcher[F]): Resource[F, MusicPlayer[F]] =
    Resource.make(make(d))(p => Async[F].delay(p.close()))

  private def make[F[_]: Async](d: Dispatcher[F]): F[MusicPlayer[F]] =
    for
      eventHub <- Topic[F, ServerMessage]
      pos <- Ref.of[F, PlaylistIndex](BasePlaylist.NoPosition)
      songs <- Ref.of[F, Seq[PlayableTrack]](Nil)
      trackHistory <- Topic[F, TrackMeta]
      states <- Topic[F, PlayerStates.PlayerState]
      timeUpdates <- Topic[F, PlaybackEvents.TimeUpdated]
    yield MusicPlayer(eventHub, pos, songs, trackHistory, states, timeUpdates, d)

class MusicPlayer[F[_]: Async](
  eventHub: Topic[F, ServerMessage],
  pos: Ref[F, PlaylistIndex],
  songs: Ref[F, Seq[PlayableTrack]],
  trackHistory: Topic[F, TrackMeta],
  states: Topic[F, PlayerStates.PlayerState],
  timeUpdatesTopic: Topic[F, PlaybackEvents.TimeUpdated],
  d: Dispatcher[F]
) extends IPlayer[F]
  with PlaylistSupport[F, PlayableTrack]
  with ServerPlayer[F]:
  val F = Concurrent[F]
  private val defaultVolume = Volume(40)
  val playlist: PimpPlaylist[F] = PimpPlaylist[F](eventHub, pos, songs)
  val allEvents: Stream[F, ServerMessage] = eventHub.subscribe(100)
  val trackHistoryEvents = trackHistory.subscribe(100)
  private val trackPlayer = new AtomicReference[Option[TrackPlayer[F]]](None)
  // TODO: jesus fix this
  var errorOpt: Option[Throwable] = None
  private val stateUpdates = states
    .subscribe(100)
    .evalMap: e =>
      val state = PimpPlayer.playState(e)
      send(PlayStateChangedMessage(state))
//  private val timeUpdates = timeUpdatesTopic
//    .subscribe(100)
//    .evalMap: time =>
//      send(TimeUpdatedMessage(time.position))
  private val timeUpdates: Stream[F, Unit] = Stream
    .awakeEvery[F](500.millis)
    .evalMapFilter(_ => F.delay(current.map(_.position)))
    .changesBy(_.toMillis)
    .evalMap: time =>
      send(TimeUpdatedMessage(time))

  val events: Stream[F, Unit] = stateUpdates
    .merge(timeUpdates)
    .handleErrorWith(t => Stream.eval(F.delay(log.error("Music player failed.", t))))

  private def current: Option[TrackPlayer[F]] = trackPlayer.get()

  def reset(track: PlayableTrack): F[Unit] =
    playlist.set(track) >>
      play(_.current)

  def setPlaylistAndPlay(track: PlayableTrack): F[Unit] =
    playlist.set(track) >>
      playTrack(track)

  override def playTrack(songMeta: PlayableTrack): F[Unit] =
    tryInitTrackWithFallback(songMeta)
      .flatMap(_ => play())
      .handleErrorWith: t =>
        log.warn(s"Unable to play track: ${songMeta.id}", t)
        errorOpt = Some(t)
        F.raiseError(t)

  def tryInitTrackWithFallback(track: PlayableTrack): F[Unit] =
    errorOpt = None
    initTrack(track).handleErrorWith:
      case ioe: IOException if Option(ioe.getMessage).exists(_.startsWith("Pipe closed")) =>
        val id = track.id
        log.warn(s"Unable to initialize track '$id'. The stream is closed.")
        F.raiseError(ioe)
      case e: Exception =>
        log.warn(s"Failed to initialize track '${track.title}' by '${track.artist}'.", e)
        F.raiseError(e)

  /** Blocks until an [[javax.sound.sampled.AudioInputStream]] can be created of `track`.
    *
    * @param track
    *   track to play
    * @throws LineUnavailableException
    *   during track init
    */
  private def initTrack(track: PlayableTrack): F[Unit] =
    val initialVolume = current.flatMap(_.volumeCarefully).getOrElse(defaultVolume)
    val initialMute = current.flatMap(_.muteCarefully).getOrElse(false)
    initPlayer(track, initialVolume, initialMute).flatMap: np =>
      val oldPlayer = trackPlayer.getAndSet(Option(np))
      oldPlayer.foreach: old =>
        old.close()
      send(TrackChangedMessage(track)) >>
        trackHistory.publish1(track).void

  def play(): F[Unit] =
    val mustReinitializePlayer = current.exists(_.state == PlayerStates.Closed)
    if mustReinitializePlayer then current.map(_.track).foreach(initTrack)
    current.map(c => c.play()).getOrElse(F.unit)

  private def initPlayer(
    track: PlayableTrack,
    initialVolume: Volume,
    isMute: Boolean
  ): F[TrackPlayer[F]] =
    val pp = track.buildPlayer(states, timeUpdatesTopic, d, () => nextTrack())
    val p = TrackPlayer(pp, eventHub)
    for
      _ <- p.adjustVolume(initialVolume)
      _ <- p.mute(isMute)
    yield p

  def stop(): F[Unit] = current.map(_.stop()).getOrElse(F.unit)

  def send(json: ServerMessage): F[Unit] = eventHub.publish1(json).void

  def seek(pos: Duration): Unit = current.foreach(_.seek(pos))

  def trySeek(pos: Duration): F[Unit] = current
    .map(_.trySeek(pos))
    .getOrElse(F.raiseError(new Exception(s"Cannot seek to '$pos', no player available.")))

  def volume(level: Int): Unit = setVolume(Volume(level))

  def volume: Option[Volume] = current.map(_.volume)

  /** @param level
    *   new volume
    * @return
    *   true if the volume was changed, false otherwise
    */
  def setVolume(level: Volume): Unit = current.foreach(_.adjustVolume(level))

  def mute(mute: Boolean): Unit = current.foreach(_.mute(mute))

  def toggleMute(): Unit = current.foreach(_.toggleMute())

  def close(): Unit =
    current.foreach(_.close())
    playlist.close()

  def position =
    current
      .map(_.position)
      .getOrElse:
        log.debug(s"Unable to obtain position because no player is initialized, defaulting to 0.")
        Duration.fromNanos(0)

  def status(host: FullUrl): F[StatusEvent] = playlist.snapshot.map: state =>
    status(host, state)

  def status(host: FullUrl, playlist: PlaylistState[PlayableTrack]): StatusEvent =
    current.fold(StatusEvents.empty): c =>
      val p = c.player
      StatusEvent(
        TrackJson.toFull(p.track, host),
        p.playState,
        p.position,
        Volume(p.volume),
        p.mute,
        playlist.songs.map(t => TrackJson.toFull(t, host)),
        playlist.index
      )

  def status17(host: FullUrl): F[StatusEvent17] = playlist.snapshot.map: state =>
    status17(host, state)

  def status17(host: FullUrl, playlist: PlaylistState[PlayableTrack]): StatusEvent17 =
    current.fold(StatusEvent17.empty): c =>
      val p = c.player
      val meta = p.track
      StatusEvent17(
        id = p.track.id,
        title = meta.title,
        artist = meta.artist,
        album = meta.album,
        state = p.playState,
        position = p.position,
        duration = p.duration,
        gain = 1.0f * p.volume / 100,
        mute = p.mute,
        playlist = playlist.songs.map(t => TrackJson.toFull(t, host)),
        index = playlist.index
      )
