package com.malliina.musicpimp.audio

import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.PimpJson
import com.malliina.musicpimp.library.LocalTrack
import play.api.libs.json.{Json, OWrites, Writes}

import scala.concurrent.duration.Duration

case class StatusEvent(track: TrackMeta,
                       state: PlayState,
                       position: Duration,
                       volume: Int,
                       mute: Boolean,
                       playlist: Seq[TrackMeta],
                       index: Int)

object StatusEvent {
  implicit val dur = JsonFormats.durationFormat
  implicit val playerState = PimpJson.playStateFormat

  implicit def status18writer(implicit w: Writes[TrackMeta]): OWrites[StatusEvent] =
    Json.writes[StatusEvent]

  val empty = StatusEvent(
    LocalTrack.empty,
    PlayState.closed,
    position = Duration.fromNanos(0),
    volume = 40,
    mute = false,
    playlist = Seq.empty,
    index = -1
  )
}
