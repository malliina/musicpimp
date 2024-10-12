package com.malliina.musicpimp.audio

import com.malliina.audio.AudioImplicits.*
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.JsonStrings.*
import com.malliina.musicpimp.models.TrackID
import io.circe.{Encoder, Json}
import io.circe.syntax.EncoderOps

import scala.concurrent.duration.Duration

case class StatusEvent17(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  state: PlayState,
  position: Duration,
  duration: Duration,
  gain: Float,
  mute: Boolean,
  playlist: Seq[FullTrack],
  index: Int
)

object StatusEvent17:
  implicit def status17writer: Encoder[StatusEvent17] =
    Encoder[StatusEvent17]: o =>
      Json.obj(
        EventKey -> StatusKey.asJson,
        Id -> o.id.asJson,
        Title -> o.title.asJson,
        Artist -> o.artist.asJson,
        Album -> o.album.asJson,
        State -> o.state.toString.asJson,
        Pos -> o.position.readable.asJson,
        PosSeconds -> o.position.toSeconds.asJson,
        DurationKey -> o.duration.readable.asJson,
        DurationSeconds -> o.duration.toSeconds.asJson,
        Gain -> (o.gain * 100).toInt.asJson,
        Mute -> o.mute.asJson,
        Playlist -> o.playlist.asJson,
        PlaylistIndexv17v18 -> o.index.asJson
      )

  val empty = StatusEvent17(
    TrackID(""),
    "",
    "",
    "",
    Closed,
    Duration.fromNanos(0),
    Duration.fromNanos(0),
    0f,
    false,
    Seq.empty,
    0
  )
