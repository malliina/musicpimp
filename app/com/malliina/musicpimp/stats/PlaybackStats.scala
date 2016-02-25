package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta

import scala.concurrent.Future

trait MostPlayedEntry {
  def track: TrackMeta

  def playbackCount: Int
}

trait PlaybackStats {
  /** Increments the playback count of `track` by one.
    *
    * @param track track being played
    * @return success on successful registration, failure otherwise
    */
  def played(track: TrackMeta): Future[Unit]

  /**
    * @return the most played tracks, ordered by playback count
    */
  def mostPlayed(): Future[Seq[MostPlayedEntry]]

  /**
    * @param count maximum number of tracks
    * @return the most recently played tracks
    */
  def mostRecent(count: Int): Future[Seq[TrackMeta]]
}
