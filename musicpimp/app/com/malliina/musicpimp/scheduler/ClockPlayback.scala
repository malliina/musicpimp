package com.malliina.musicpimp.scheduler

import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.{FullTrack, TrackJson}
import play.api.libs.json.Json

case class TrackJob(track: Option[FullTrack])

object TrackJob {
  implicit val json = Json.format[TrackJob]
}

case class FullClockPlayback(id: Option[String], job: TrackJob, when: ClockSchedule, enabled: Boolean)

object FullClockPlayback {
  implicit val json = Json.format[FullClockPlayback]
}

case class ClockPlayback(id: Option[String],
                         job: PlaybackJob,
                         when: ClockSchedule,
                         enabled: Boolean)
  extends PlaybackAP[ClockSchedule] {
  def toFull(host: FullUrl): FullClockPlayback =
    FullClockPlayback(id, TrackJob(job.trackInfo.map(TrackJson.toFull(_, host))), when, enabled)
}

object ClockPlayback {
  val shortJson = Json.format[ClockPlayback]
}
