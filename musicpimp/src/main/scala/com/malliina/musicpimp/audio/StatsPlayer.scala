package com.malliina.musicpimp.audio

import com.malliina.musicpimp.db.DoobieUserManager
import com.malliina.musicpimp.stats.PlaybackStats
import com.malliina.values.Username
import cats.effect.{Async, Ref}
import cats.implicits.{toFlatMapOps, toFunctorOps}

object StatsPlayer:
  def default[F[_]: Async](player: MusicPlayer[F], stats: PlaybackStats[F]) =
    Ref
      .of[F, Username](DoobieUserManager.defaultUser)
      .map: ref =>
        StatsPlayer(ref, player, stats)

/** Mediator that keeps track of who is controlling the player, for statistics.
  *
  * @param stats
  *   stats database
  */
class StatsPlayer[F[_]: Async](
  latestUser: Ref[F, Username],
  player: MusicPlayer[F],
  stats: PlaybackStats[F]
):
  val subscription = player.trackHistoryEvents.evalMap: track =>
    latestUser.get.flatMap: user =>
      stats.played(track, user)

  def updateUser(user: Username): F[Unit] = latestUser.set(user)
