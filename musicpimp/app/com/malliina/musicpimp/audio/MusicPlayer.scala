package com.malliina.musicpimp.audio

import java.io.IOException
import java.util.concurrent.atomic.AtomicReference

import akka.NotUsed
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import com.malliina.audio._
import com.malliina.http.FullUrl
import com.malliina.musicpimp.models.Volume
import com.malliina.streams.StreamsUtil
import javax.sound.sampled.LineUnavailableException
import play.api.Logger

import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

/** This is a mutable mess. It should be rewritten, maybe using Rx.
  */
class MusicPlayer()(implicit val mat: Materializer)
  extends IPlayer
  with PlaylistSupport[PlayableTrack]
  with ServerPlayer {

  private val log = Logger(getClass)
  private val defaultVolume = Volume(40)
  val playlist: PimpPlaylist = new PimpPlaylist

  private val eventHub = StreamsUtil.connectedStream[ServerMessage]()
  val allEvents: Source[ServerMessage, NotUsed] = eventHub.source.merge(playlist.events)
  val trackHistoryHub = StreamsUtil.connectedStream[TrackMeta]()
  private val trackPlayer = new AtomicReference[Option[TrackPlayer]](None)
  // TODO: jesus fix this
  var errorOpt: Option[Throwable] = None

  private def current: Option[TrackPlayer] = trackPlayer.get()

  def reset(track: PlayableTrack): Try[Unit] = {
    playlist.set(track)
    play(_.current)
  }

  def setPlaylistAndPlay(track: PlayableTrack): Try[Unit] = {
    playlist.set(track)
    playTrack(track)
  }

  override def playTrack(songMeta: PlayableTrack): Try[Unit] =
    tryInitTrackWithFallback(songMeta).map(_ => play()).recoverWith {
      case t =>
        log.warn(s"Unable to play track: ${songMeta.id}", t)
        errorOpt = Some(t)
        Failure(t)
    }

  def tryInitTrackWithFallback(track: PlayableTrack): Try[Unit] = {
    errorOpt = None
    Try(initTrack(track)).recoverWith {
      case ioe: IOException if Option(ioe.getMessage).exists(_.startsWith("Pipe closed")) =>
        val id = track.id
        log.warn(s"Unable to initialize track '$id'. The stream is closed.")
        Failure(ioe)
    }
  }

  /** Blocks until an [[javax.sound.sampled.AudioInputStream]] can be created of `track`.
    *
    * @param track track to play
    * @throws LineUnavailableException during track init
    */
  def initTrack(track: PlayableTrack): Unit = {
    val initialVolume = current.flatMap(_.volumeCarefully) getOrElse defaultVolume
    val initialMute = current.flatMap(_.muteCarefully) getOrElse false
    val newPlayer = initPlayer(track, initialVolume, initialMute)
    val oldPlayer = trackPlayer.getAndSet(Option(newPlayer))
    oldPlayer foreach { old =>
      old.close()
    }
    send(TrackChangedMessage(track))
    trackHistoryHub.send(track)
  }

  def play(): Unit = {
    val mustReinitializePlayer = current.exists(_.state == PlayerStates.Closed)
    if (mustReinitializePlayer) {
      current.map(_.track).foreach(initTrack)
    }
    current.foreach { p =>
      p.play()
    }
  }

  def initPlayer(track: PlayableTrack, initialVolume: Volume, isMute: Boolean): TrackPlayer = {
    val p = new TrackPlayer(track.buildPlayer(() => nextTrack()), eventHub.sink)
    p.adjustVolume(initialVolume)
    p.mute(isMute)
    p
  }

  def stop(): Unit = current.foreach(_.stop())

  def send(json: ServerMessage): Unit = eventHub.sink.send(json)

  def seek(pos: Duration): Unit = current.foreach(_.seek(pos))

  def trySeek(pos: Duration): Try[Unit] = current
    .map(_.trySeek(pos))
    .getOrElse(Failure(new Exception(s"Cannot seek to '$pos', no player available.")))

  def volume(level: Int): Unit = setVolume(Volume(level))

  def volume: Option[Volume] = current.map(_.volume)

  /**
    *
    * @param level new volume
    * @return true if the volume was changed, false otherwise
    */
  def setVolume(level: Volume): Unit = current.foreach(_.adjustVolume(level))

  def mute(mute: Boolean): Unit = current.foreach(_.mute(mute))

  def toggleMute(): Unit = current.foreach(_.toggleMute())

  def close(): Unit = {
    current.foreach(_.close())
    eventHub.shutdown()
    trackHistoryHub.shutdown()
    playlist.close()
  }

  def position =
    current.map(_.position).getOrElse {
      log.debug(s"Unable to obtain position because no player is initialized, defaulting to 0.")
      Duration.fromNanos(0)
    }

  def status(host: FullUrl): StatusEvent = current.fold(StatusEvents.empty) { c =>
    val p = c.player
    StatusEvent(
      TrackJson.toFull(p.track, host),
      p.playState,
      p.position,
      Volume(p.volume),
      p.mute,
      playlist.songList.map(t => TrackJson.toFull(t, host)),
      playlist.index
    )
  }

  def status17(host: FullUrl): StatusEvent17 =
    current.fold(StatusEvent17.empty) { c =>
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
        playlist = playlist.songList.map(t => TrackJson.toFull(t, host)),
        index = playlist.index
      )
    }
}
