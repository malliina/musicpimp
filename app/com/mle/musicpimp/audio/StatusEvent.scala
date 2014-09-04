package com.mle.musicpimp.audio

import com.mle.audio.PlayerStates
import com.mle.musicpimp.json.PimpJson
import com.mle.musicpimp.library.LocalTrack
import com.mle.play.json.JsonFormats
import com.mle.util.Log
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

/**
 *
 * @author mle
 */
case class StatusEvent(track: TrackMeta,
                       state: PlayerStates.PlayerState,
                       position: Duration,
                       volume: Int,
                       mute: Boolean,
                       playlist: Seq[TrackMeta],
                       index: Int)

object StatusEvent extends Log {
  implicit val dur = JsonFormats.durationFormat
  implicit val playerState = PimpJson.playStateFormat
  implicit val status18writer = Json.writes[StatusEvent]

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