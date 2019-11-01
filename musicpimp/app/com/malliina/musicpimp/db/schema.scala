package com.malliina.musicpimp.db

import com.malliina.musicpimp.models.PlaylistID
import com.malliina.values.Username
import io.getquill.Embedded

import scala.concurrent.duration.FiniteDuration

// Row is taken
case class PlaylistRecord(id: PlaylistID, name: String, user: Username)
case class PlaylistTotals(id: PlaylistID, tracks: Long, duration: FiniteDuration) extends Embedded
