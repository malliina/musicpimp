package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.Volume
import play.api.libs.json.{Json, OFormat}

import scala.concurrent.duration.Duration

case class StatusEvent(track: FullTrack,
                       state: PlayState,
                       position: Duration,
                       volume: Volume,
                       mute: Boolean,
                       playlist: Seq[FullTrack],
                       index: Int)

object StatusEvent {
  implicit val dur: CrossFormats.duration.type = CrossFormats.duration
  implicit val json: OFormat[StatusEvent] = Json.format[StatusEvent]
}
