package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.values.Username

trait PlaybackStats[F[_]]:

  /** Increments the playback count of `track` by one.
    *
    * @param track
    *   track being played
    * @param user
    *   playing user
    * @return
    *   success on successful registration, failure otherwise
    */
  def played(track: TrackMeta, user: Username): F[Unit]

  /** Returns the most played tracks.
    *
    * The returned entries are sorted by playback count primarily, and latest playback timestamp
    * secondarily (latest first). This provides deterministically sorted results also in cases where
    * the playback counts of two tracks equal. Deterministic sorting is required for proper paging
    * support.
    *
    * @param request
    *   request limits
    * @return
    *   the most played tracks, ordered by playback count
    */
  def mostPlayed(request: DataRequest): F[List[PopularEntry]]

  /** @param request
    *   request limits
    * @return
    *   the most recently played tracks
    */
  def mostRecent(request: DataRequest): F[List[RecentEntry]]
