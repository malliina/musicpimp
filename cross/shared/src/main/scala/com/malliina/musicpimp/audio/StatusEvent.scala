package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.Volume
import play.api.libs.json.Json

import scala.concurrent.duration.Duration

case class StatusEvent(track: FullTrack,
                       state: PlayState,
                       position: Duration,
                       volume: Volume,
                       mute: Boolean,
                       playlist: Seq[FullTrack],
                       index: Int)

object StatusEvent {
  implicit val dur = CrossFormats.duration
  implicit val json = Json.format[StatusEvent]
}
