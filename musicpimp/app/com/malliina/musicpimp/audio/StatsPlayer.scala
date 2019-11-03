package com.malliina.musicpimp.audio

import akka.stream.KillSwitches
import akka.stream.scaladsl.{Keep, Sink}
import com.malliina.musicpimp.db.NewUserManager
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.values.Username

import scala.concurrent.stm.{Ref, atomic}

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats stats database
  */
class StatsPlayer(player: MusicPlayer, stats: PlaybackStats) extends AutoCloseable {
  implicit val mat = player.mat
  val latestUser = Ref[Username](NewUserManager.defaultUser)
  val subscription = player.trackHistory
    .viaMat(KillSwitches.single)(Keep.right)
    .to(Sink.foreach { track =>
      val user = latestUser.single.get
      stats.played(track, user)
    })
    .run()

  def updateUser(user: Username): Unit =
    atomic(txn => latestUser.update(user)(txn))

  def close(): Unit =
    subscription.shutdown()
}
