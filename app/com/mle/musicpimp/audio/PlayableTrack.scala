package com.mle.musicpimp.audio

/**
 *
 * @author mle
 */
trait PlayableTrack extends TrackMeta {
  def buildPlayer(eom: () => Unit): PimpPlayer
}
