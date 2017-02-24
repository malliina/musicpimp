package com.malliina.musicpimp.audio

import java.io.IOException
import javax.sound.sampled.LineUnavailableException

import com.malliina.audio._
import com.malliina.musicpimp.audio.ServerMessage._
import com.malliina.musicpimp.library.Library
import com.malliina.util.Log
import rx.lang.scala.{Observable, Subject, Subscription}

import scala.concurrent.duration.Duration
import scala.util.{Failure, Try}

/** This is a mutable mess. It should be rewritten, maybe using Rx.
  */
object MusicPlayer
  extends IPlayer
    with PlaylistSupport[PlayableTrack]
    with ServerPlayer
    with Log {
  private val defaultVolume = 40
  val playlist: PimpPlaylist = new PimpPlaylist

  private val subject = Subject[ServerMessage]()
  val events: Observable[ServerMessage] = subject
  val allEvents = events.merge(playlist.events)
  private val trackHistorySubject = Subject[TrackMeta]()
  val trackHistory: Observable[TrackMeta] = trackHistorySubject

  // TODO: jesus fix this
  var errorOpt: Option[Throwable] = None

  var stateSubscription: Option[Subscription] = None
  var timeSubscription: Option[Subscription] = None

  /** Every time the track changes, a new [[PimpPlayer]] is used.
    * - Well, why?
    */
  private var player: Option[PimpPlayer] = None

  def underLying = player

  def reset(track: PlayableTrack): Try[Unit] = {
    playlist set track
    play(_.current)
  }

  def setPlaylistAndPlay(track: PlayableTrack): Try[Unit] = {
    playlist set track
    playTrack(track)
  }

  override def playTrack(songMeta: PlayableTrack): Try[Unit] = {
    tryInitTrackWithFallback(songMeta).map(_ => play()).recoverWith {
      case t =>
        log.warn(s"Unable to play track: ${songMeta.id}", t)
        errorOpt = Some(t)
        Failure(t)
    }
  }

  def tryInitTrackWithFallback(track: PlayableTrack): Try[Unit] = {
    errorOpt = None
    Try(initTrack(track)).recoverWith {
      case ioe: IOException if Option(ioe.getMessage).exists(_.startsWith("Pipe closed")) =>
        val id = track.id
        log.warn(s"Unable to initialize track: $id. The stream is closed. Trying to reinitialize.")
        Library.findMetaWithTempFallback(id)
          .map(newTrack => Try(initTrack(newTrack)))
          .getOrElse(Failure(ioe))
    }
  }


  /** Blocks until an [[javax.sound.sampled.AudioInputStream]] can be created of the media.
    *
    * @param track
    * @throws LineUnavailableException
    */
  private def initTrack(track: PlayableTrack): Unit = {
    // If the player exists, tries to obtain the volume; if it fails, falls back to the cached volume.
    val previousVolume: Option[Int] = tryWithFallback(_.volume, _.cachedVolume)
    val previousMute: Option[Boolean] = tryWithFallback(_.mute, _.cachedMute)
    close()
    player = None
    //    log.info(s"Closed track, now initializing: ${track.title}")
    // PimpJavaSoundPlayer.ctor throws at least LineUnavailableException if the audio device cannot be initialized
    player = Some(initPlayer(track))
    // Maintains the gain & mute status as they were in the previous track.
    // If there was no previous gain, there was no previous track, so we set the default volume.
    val volumeChanged = setVolume(previousVolume getOrElse defaultVolume)
    // ensures the volume message is always sent
    if (!volumeChanged) {
      sendCurrentVolume()
    }
    previousMute foreach mute
    send(TrackChangedMessage(track))
    trackHistorySubject.onNext(track)
  }

  /** If the player exists, first tries `first`, and if that fails exceptionally, falls back to `fallback`.
    *
    * @param first    first attempt
    * @param fallback optional fallback value
    * @tparam T desired result
    * @return a result wrapped in an [[Option]]
    */
  private def tryWithFallback[T](first: PimpPlayer => T, fallback: PimpPlayer => Option[T]): Option[T] =
    player.flatMap(p => Try(first(p)).toOption.orElse(fallback(p)))

  def play() = {
    val mustReinitializePlayer = player.exists(_.state == PlayerStates.Closed)
    if (mustReinitializePlayer) {
      player = player.map(p => initPlayer(p.track))
    }
    player.foreach(p => {
      p.play()
    })
  }

  def initPlayer(track: PlayableTrack): PimpPlayer = {
    close()
    val newPlayer = track.buildPlayer(() => nextTrack())
    stateSubscription = Some(newPlayer.events.subscribe { playerState =>
      send(PlayStateChangedMessage(playerState))
    })
    timeSubscription = Some(newPlayer.timeUpdates.subscribe { time =>
      send(TimeUpdatedMessage(time.position))
    })
    newPlayer
  }

  def stop() {
    player.foreach(p => {
      p.stop()
      send(PlayStateChangedMessage(PlayerStates.Stopped))
    })
  }

  // TODO observables
  def send(json: ServerMessage) = subject.onNext(json)

  def seek(pos: Duration) = {
    Try {
      player.filter(_.position.toSeconds != pos.toSeconds).foreach(p => {
        p.seek(pos)
        send(TimeUpdatedMessage(pos))
      })
    }.recover {
      case ioe: IOException if ioe.getMessage == "Resetting to invalid mark" =>
        log.warn(s"Failed to seek to: $pos. Unable to reset stream.")
    }
  }

  def volume(level: Int): Unit = setVolume(level)

  def volume: Option[Int] = player.map(_.volume)

  /**
    *
    * @param level new volume
    * @return true if the volume was changed, false otherwise
    */
  def setVolume(level: Int): Boolean =
    player.filter(_.volume != level).map(p => {
      p.volume = level
      sendVolumeChanged(level)
    }).isDefined

  def sendVolumeChanged(level: Int) = send(VolumeChangedMessage(level))

  def sendCurrentVolume() = sendVolumeChanged(player.map(_.volume) getOrElse 0)

  def mute(mute: Boolean) {
    player.filter(_.mute != mute).foreach(p => {
      p.mute(mute)
      send(MuteToggledMessage(mute))
    })
  }

  def toggleMute() {
    player.foreach(p => {
      p.toggleMute()
      send(MuteToggledMessage(p.mute))
    })
  }

  def close(): Unit = {
    stateSubscription.foreach(_.unsubscribe())
    stateSubscription = None
    timeSubscription.foreach(_.unsubscribe())
    timeSubscription = None
    player.foreach(p => {
      p.close()
      p.media.stream.close()
    })
  }


  def position =
    player.map(_.position).getOrElse {
      log.debug(s"Unable to obtain position because no player is initialized, defaulting to 0.")
      Duration.fromNanos(0)
    }

  def status: StatusEvent = player.map(p => StatusEvent(
    p.track,
    p.state,
    p.position,
    p.volume,
    p.mute,
    playlist.songList,
    playlist.index
  )).getOrElse(StatusEvent.empty)

  def status17: StatusEvent17 = {
    player.map(p => {
      val meta = p.track
      StatusEvent17(
        id = p.track.id,
        title = meta.title,
        artist = meta.artist,
        album = meta.album,
        state = p.state,
        position = p.position,
        duration = p.duration,
        gain = 1.0f * p.volume / 100,
        mute = p.mute,
        playlist = playlist.songList,
        index = playlist.index
      )
    }).getOrElse(StatusEvent17.noServerTrackEvent)
  }
}

