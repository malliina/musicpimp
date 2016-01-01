package com.malliina.musicpimp.audio

import com.malliina.audio.IPlaylist

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
  def playTrack(song: T)

  /**
   * Skips to the track with the specified index; playback starts automatically.
   *
   * @param index track index
   * @return the track skipped to
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  def skip(index: Int): Unit = {
    playlist.index = index
    playlist.current.foreach(playTrack)
  }

  def nextTrack() = play(_.next)

  def previousTrack() = play(_.prev)

  private def play(f: IPlaylist[T] => Option[T]) = f(playlist).foreach(playTrack)
}
