package com.mle.musicpimp.audio

import com.mle.musicpimp.library.TrackInfo
import com.mle.audio.PlayerStates
import scala.concurrent.duration.Duration
import com.mle.util.Log
import play.api.libs.json.Json
import com.mle.audio.meta.SongTags
import play.api.libs.json.Json._
import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.json.PimpJson._

/**
 *
 * @author mle
 */
case class StatusEvent(track: TrackInfo,
                       state: PlayerStates.PlayerState,
                       position: Duration,
                       volume: Int,
                       mute: Boolean,
                       playlist: Seq[TrackInfo],
                       index: Int)

object StatusEvent extends Log {
  implicit val status18writer = Json.writes[StatusEvent]

  val empty = StatusEvent(
    TrackInfo.empty,
    PlayerStates.Closed,
    position = Duration.fromNanos(0),
    volume = 40,
    mute = false,
    playlist = Seq.empty,
    index = -1
  )

  def shortJson(tags: SongTags) = toJson(Map(
    TITLE -> tags.title,
    ALBUM -> tags.album,
    ARTIST -> tags.artist
  ))

  def webPlaylistJson(user: String) = {
    val player = WebPlayback.players.get(user) getOrElse new PimpWebPlayer(user)
    obj(
      PLAYLIST -> toJson(player.playlist.songList),
      PLAYLIST_INDEX -> toJson(player.playlist.index)
    )
  }
}