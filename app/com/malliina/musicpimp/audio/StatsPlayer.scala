package com.malliina.musicpimp.audio

import com.malliina.musicpimp.db.DatabaseUserManager
import com.malliina.musicpimp.models.User
import com.malliina.musicpimp.stats.PlaybackStats

import scala.concurrent.stm.{Ref, atomic}

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats stats database
  */
class StatsPlayer(stats: PlaybackStats) extends AutoCloseable {
  val latestUser = Ref[User](DatabaseUserManager.DefaultUser)
  val player = MusicPlayer
  val subscription = MusicPlayer.trackHistory.subscribe(
    track => stats.played(track, latestUser.single.get),
    err => (),
    () => ()
  )

  def updateUser(user: User): Unit = {
    atomic(txn => latestUser.update(user)(txn))
  }

  def close(): Unit =
    subscription.unsubscribe()
}
