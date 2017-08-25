package com.malliina.musicpimp.audio

import com.malliina.audio.AudioImplicits._
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.TrackID
import play.api.libs.json.Json._
import play.api.libs.json.{OWrites, Writes}

import scala.concurrent.duration.Duration

case class StatusEvent17(id: TrackID,
                         title: String,
                         artist: String,
                         album: String,
                         state: PlayState,
                         position: Duration,
                         duration: Duration,
                         gain: Float,
                         mute: Boolean,
                         playlist: Seq[TrackMeta],
                         index: Int)

object StatusEvent17 {
  implicit def status17writer(implicit w: Writes[TrackMeta]): OWrites[StatusEvent17] =
    OWrites[StatusEvent17] { o =>
      obj(
        EventKey -> StatusKey,
        Id -> TrackID.format.writes(o.id),
        Title -> o.title,
        Artist -> o.artist,
        Album -> o.album,
        State -> o.state.toString,
        Pos -> toJson(o.position.readable),
        PosSeconds -> toJson(o.position.toSeconds),
        DurationKey -> toJson(o.duration.readable),
        DurationSeconds -> toJson(o.duration.toSeconds),
        Gain -> toJson((o.gain * 100).toInt),
        Mute -> toJson(o.mute),
        Playlist -> toJson(o.playlist),
        PlaylistIndexv17v18 -> toJson(o.index)
      )
    }

  val empty = StatusEvent17(
    TrackID(""),
    "",
    "",
    "",
    Closed,
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
