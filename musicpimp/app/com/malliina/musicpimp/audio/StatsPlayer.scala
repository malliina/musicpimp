package com.malliina.musicpimp.audio

import cats.effect.IO
import com.malliina.concurrent.Execution.runtime
import com.malliina.musicpimp.db.DoobieUserManager
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.values.Username
import org.apache.pekko.stream.scaladsl.{Keep, Sink}
import org.apache.pekko.stream.{KillSwitches, Materializer}

import scala.concurrent.stm.{Ref, atomic}

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats
  *   stats database
  */
class StatsPlayer(player: MusicPlayer, stats: PlaybackStats[IO]) extends AutoCloseable:
  implicit val mat: Materializer = player.mat
  val latestUser = Ref[Username](DoobieUserManager.defaultUser)
  val subscription = player.trackHistoryHub.source
    .viaMat(KillSwitches.single)(Keep.right)
    .to(Sink.foreach: track =>
      val user = latestUser.single.get
      stats.played(track, user).unsafeToFuture()
    )
    .run()

  def updateUser(user: Username): Unit =
    atomic(txn => latestUser.update(user)(txn))

  def close(): Unit =
    subscription.shutdown()
