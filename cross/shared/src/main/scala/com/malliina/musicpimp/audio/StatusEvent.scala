package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.Volume
import play.api.libs.json._

import scala.concurrent.duration.Duration

case class StatusEvent(track: TrackMeta,
                       state: PlayState,
                       position: Duration,
                       volume: Volume,
                       mute: Boolean,
                       playlist: Seq[TrackMeta],
                       index: Int)

object StatusEvent {
  implicit val dur = CrossFormats.duration
  implicit val reader = Json.reads[StatusEvent]

  implicit def status18writer(implicit w: Writes[TrackMeta]): OFormat[StatusEvent] =
    Json.format[StatusEvent]
}
