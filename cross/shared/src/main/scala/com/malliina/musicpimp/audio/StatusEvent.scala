package com.malliina.musicpimp.audio

import com.malliina.json.PrimitiveFormats
import com.malliina.musicpimp.models.Volume
import io.circe.Codec
import io.circe.generic.semiauto.deriveCodec

import scala.concurrent.duration.Duration

case class StatusEvent(
  track: FullTrack,
  state: PlayState,
  position: Duration,
  volume: Volume,
  mute: Boolean,
  playlist: Seq[FullTrack],
  index: Int
)

object StatusEvent:
  implicit val dur: Codec[Duration] = PrimitiveFormats.durationCodec
  implicit val json: Codec[StatusEvent] = deriveCodec[StatusEvent]
