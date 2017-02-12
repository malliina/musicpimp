package com.malliina.musicpimp.audio

trait PlayableTrack extends TrackMeta {
  def buildPlayer(eom: () => Unit): PimpPlayer
}
