package com.malliina.musicpimp.audio

import com.malliina.audio.AudioImplicits._
import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.json.JsonStrings._
import play.api.libs.json.Json._
import play.api.libs.json.{JsValue, Writes}

import scala.concurrent.duration.Duration

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
                         playlist: Seq[TrackMeta],
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
      PLAYLIST_INDEXv17v18 -> toJson(o.index)
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
