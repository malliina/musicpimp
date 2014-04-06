package com.mle.musicpimp.audio

import com.mle.musicpimp.library.TrackInfo
import com.mle.audio.PlayerStates
import scala.concurrent.duration.Duration
import com.mle.util.Log
import play.api.libs.json.Json
import com.mle.play.json.JsonFormats2
import com.mle.musicpimp.json.PimpJson

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
  implicit val dur = JsonFormats2.durationFormat
  implicit val playerState = PimpJson.playStateFormat
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
}