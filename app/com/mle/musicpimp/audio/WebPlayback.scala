package com.mle.musicpimp.audio

import com.mle.musicpimp.library.LocalTrack

import scala.collection.mutable

/**
 *
 * @author mle
 */
trait WebPlayback {
  val players = mutable.Map.empty[String, PimpWebPlayer]

  def player(user: String) =
    players.getOrElseUpdate(user, new PimpWebPlayer(user))

  def add(user: String, track: TrackMeta) {
    val p = player(user)
    p.playlist add track
  }

  def remove(user: String, trackIndex: Int): Unit =
    players.get(user).map(_.playlist.delete(trackIndex))

  def execute(user: String, op: PimpWebPlayer => Unit): Unit =
    op(player(user))
}

object WebPlayback extends WebPlayback
