package com.mle.musicpimp.audio

import com.mle.audio.IPlaylist

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
   * Skips to the track with the specified index;
   * playback starts automatically.
   *
   * @param index track index
   * @return the track skipped to
   * @throws IndexOutOfBoundsException if the index is out of bounds
   */
  def skip(index: Int): Unit = {
    playlist.index = index
    playlist.current.map(playTrack)
  }

  def nextTrack() {
    playlist.next.foreach(playTrack)
  }

  def previousTrack() {
    playlist.prev.foreach(playTrack)
  }
}
