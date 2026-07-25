package com.malliina.musicpimp.audio

import cats.effect.kernel.Async
import cats.implicits.{catsSyntaxFlatMapOps, toFlatMapOps}
import com.malliina.audio.{IPlaylist, PlaylistIndex}

trait PlaylistSupport[F[_]: Async, T]:
  def playlist: IPlaylist[F, T]

  /** Initializes the player with the given track.
    *
    * Does not modify the playlist; it is assumed the supplied track is part of the playlist.
    */
  def playTrack(song: T): F[Unit]

  /** Skips to the track with the specified index; playback starts automatically.
    *
    * @param index
    *   track index
    * @return
    *   the track skipped to
    * @throws IndexOutOfBoundsException
    *   if the index is out of bounds
    */
  def skip(index: PlaylistIndex): F[Unit] =
    playlist.setIndex(index) >> play(_.current)

  def nextTrack() = play(_.next): F[Unit]

  def previousTrack() = play(_.prev): F[Unit]

  protected def play(f: IPlaylist[F, T] => F[Option[T]]): F[Unit] =
    f(playlist).flatMap: opt =>
      opt.map(playTrack).getOrElse(Async[F].raiseError(new Exception("No track")))
