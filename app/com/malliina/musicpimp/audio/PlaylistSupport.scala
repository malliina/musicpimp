package com.malliina.musicpimp.audio

import com.malliina.audio.IPlaylist

import scala.util.{Failure, Try}

/**
 * @author Michael
 */
trait PlaylistSupport[T] {
  def playlist: IPlaylist[T]

  /**
   * Initializes the player with the given track.
   *
   * Does not modify the playlist; it is assumed the supplied track is part of the playlist.
   *
   * @param song
   */
  def playTrack(song: T): Try[Unit]

  /**
   * Skips to the track with the specified index; playback starts automatically.
   *
   * @param index track index
   * @return the track skipped to
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  def skip(index: Int): Try[Unit] = {
    playlist.index = index
    play(_.current)
  }

  def nextTrack() = play(_.next): Try[Unit]

  def previousTrack() = play(_.prev): Try[Unit]

  protected def play(f: IPlaylist[T] => Option[T]): Try[Unit] =
    f(playlist).map(playTrack).getOrElse(Failure(new Exception("No track")))
}
