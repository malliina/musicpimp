package com.malliina.musicpimp.audio

import akka.stream.Materializer

trait PlayableTrack extends TrackMeta {
  def buildPlayer(eom: () => Unit)(implicit mat: Materializer): PimpPlayer
}
