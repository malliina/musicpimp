package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.json.JsonFormats
import com.malliina.musicpimp.json.PimpJson
import com.malliina.musicpimp.library.LocalTrack
import play.api.libs.json.{Json, Writes}

import scala.concurrent.duration.Duration

case class StatusEvent(track: TrackMeta,
                       state: PlayerStates.PlayerState,
                       position: Duration,
                       volume: Int,
                       mute: Boolean,
                       playlist: Seq[TrackMeta],
                       index: Int)

object StatusEvent {
  implicit val dur = JsonFormats.durationFormat
  implicit val playerState = PimpJson.playStateFormat

  implicit def status18writer(implicit w: Writes[TrackMeta]) = {
    Json.writes[StatusEvent]
  }

  val empty = StatusEvent(
    LocalTrack.empty,
    PlayerStates.Closed,
    position = Duration.fromNanos(0),
    volume = 40,
    mute = false,
    playlist = Seq.empty,
    index = -1
  )
}
