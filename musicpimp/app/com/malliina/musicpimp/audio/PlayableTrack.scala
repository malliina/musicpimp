package com.malliina.musicpimp.audio

import org.apache.pekko.stream.Materializer

trait PlayableTrack extends TrackMeta:
  def buildPlayer(eom: () => Unit)(implicit mat: Materializer): PimpPlayer
