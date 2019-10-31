package com.malliina.musicpimp.scheduler

import com.malliina.musicpimp.audio.FullTrack
import com.malliina.musicpimp.models.TrackID
import play.api.libs.json.Json

case class TrackJob(track: FullTrack)

object TrackJob {
  implicit val json = Json.format[TrackJob]
}

case class TrackWrapper(track: TrackID)

object TrackWrapper {
  implicit val json = Json.format[TrackWrapper]
}

case class FullClockPlayback(
  id: Option[String],
  job: TrackJob,
  when: ClockSchedule,
  enabled: Boolean
) {
  def describe = s"Plays ${job.track.title}"
}

object FullClockPlayback {
  implicit val json = Json.format[FullClockPlayback]
}

case class ClockPlaybackConf(
  id: Option[String],
  track: TrackID,
  when: ClockSchedule,
  enabled: Boolean
)

object ClockPlaybackConf {
  implicit val json = Json.format[ClockPlaybackConf]
}

case class ClockPlayback(
  id: Option[String],
  job: TrackWrapper,
  when: ClockSchedule,
  enabled: Boolean
) {
  def toConf = ClockPlaybackConf(id, job.track, when, enabled)
}

object ClockPlayback {
  implicit val json = Json.format[ClockPlayback]

  def fromConf(conf: ClockPlaybackConf, job: PlaybackJob) =
    apply(conf.id, TrackWrapper(job.trackId), conf.when, conf.enabled)
}
