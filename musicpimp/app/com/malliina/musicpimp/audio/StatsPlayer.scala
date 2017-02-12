package com.malliina.musicpimp.audio

import com.malliina.musicpimp.db.DatabaseUserManager
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.play.models.Username
import play.api.Logger

import scala.concurrent.stm.{Ref, atomic}

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats stats database
  */
class StatsPlayer(stats: PlaybackStats) extends AutoCloseable {
  val latestUser = Ref[Username](DatabaseUserManager.DefaultUser)
  val player = MusicPlayer
  val subscription = player.trackHistory.subscribe(
    track => {
      val user = latestUser.single.get
      stats.played(track, user)
    },
    err => (),
    () => ()
  )

  def updateUser(user: Username): Unit =
    atomic(txn => latestUser.update(user)(txn))

  def close(): Unit =
    subscription.unsubscribe()
}

object StatsPlayer {
  private val log = Logger(getClass)
}
