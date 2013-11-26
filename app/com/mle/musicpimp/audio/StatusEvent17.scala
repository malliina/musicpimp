package com.mle.musicpimp.audio

import com.mle.audio.PlayerStates
import play.api.libs.json.Json._
import com.mle.audio.AudioImplicits._
import scala.concurrent.duration.Duration
import com.mle.audio.meta.SongTags
import com.mle.util.Log
import com.mle.musicpimp.library.TrackInfo
import play.api.libs.json.{Json, JsValue, Writes}
import com.mle.musicpimp.json.JsonStrings._

/**
 * @author Michael
 */
/**
 *
 */
case class StatusEvent17(id: String,
                         title: String,
                         artist: String,
                         album: String,
                         state: PlayerStates.PlayerState,
                         position: Duration,
                         duration: Duration,
                         gain: Float,
                         mute: Boolean,
                         playlist: Seq[TrackInfo],
                         index: Int)

object StatusEvent17 {
  implicit val status17writer = new Writes[StatusEvent17] {
    def writes(o: StatusEvent17): JsValue = obj(
      EVENT -> STATUS,
      ID -> toJson(o.id),
      TITLE -> o.title,
      ARTIST -> o.artist,
      ALBUM -> o.album,
      STATE -> o.state.toString,
      POS -> toJson(o.position.readable),
      POS_SECONDS -> toJson(o.position.toSeconds),
      DURATION -> toJson(o.duration.readable),
      DURATION_SECONDS -> toJson(o.duration.toSeconds),
      GAIN -> toJson((o.gain * 100).toInt),
      MUTE -> toJson(o.mute),
      PLAYLIST -> toJson(o.playlist),
      PLAYLIST_INDEX -> toJson(o.index)
    )
  }
  val empty = StatusEvent17(
    "",
    "",
    "",
    "",
    PlayerStates.Closed,
    Duration.fromNanos(0),
    Duration.fromNanos(0),
    0F,
    false,
    Seq.empty,
    0
  )

  def noServerTrackEvent = StatusEvent17.empty.copy(
    playlist = MusicPlayer.playlist.songList,
    index = MusicPlayer.playlist.index
  )
}
