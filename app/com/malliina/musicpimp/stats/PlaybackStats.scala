package com.malliina.musicpimp.stats

import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.models.User

import scala.concurrent.Future

trait PlaybackStats {
  /** Increments the playback count of `track` by one.
    *
    * @param track track being played
    * @param user  playing user
    * @return success on successful registration, failure otherwise
    */
  def played(track: TrackMeta, user: User): Future[Unit]

  /**
    * @param request request limits
    * @return the most played tracks, ordered by playback count
    */
  def mostPlayed(request: DataRequest): Future[Seq[PopularEntry]]

  /**
    * @param request request limits
    * @return the most recently played tracks
    */
  def mostRecent(request: DataRequest): Future[Seq[RecentEntry]]
}
