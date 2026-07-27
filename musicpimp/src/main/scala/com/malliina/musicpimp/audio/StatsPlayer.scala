package com.malliina.musicpimp.audio

import com.malliina.musicpimp.db.DoobieUserManager
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.values.Username

import scala.concurrent.stm.{Ref, atomic}

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats
  *   stats database
  */
class StatsPlayer[F[_]](player: MusicPlayer[F], stats: PlaybackStats[F]) extends AutoCloseable:
  private val latestUser = Ref[Username](DoobieUserManager.defaultUser)
  val subscription = player.trackHistoryEvents.evalMap: track =>
    val user = latestUser.single.get
    stats.played(track, user)

  def updateUser(user: Username): Unit =
    atomic(txn => latestUser.update(user)(using txn))

  def close(): Unit = ()
