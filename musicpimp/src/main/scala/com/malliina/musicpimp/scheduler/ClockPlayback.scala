package com.malliina.musicpimp.scheduler

import com.malliina.musicpimp.audio.FullTrack
import com.malliina.musicpimp.models.TrackID
import io.circe.Codec

case class TrackJob(track: FullTrack) derives Codec.AsObject

case class TrackWrapper(track: TrackID) derives Codec.AsObject

case class FullClockPlayback(
  id: Option[String],
  job: TrackJob,
  when: ClockSchedule,
  enabled: Boolean
) derives Codec.AsObject:
  def describe = s"Plays ${job.track.title}"

case class ClockPlaybackConf(
  id: Option[String],
  track: TrackID,
  when: ClockSchedule,
  enabled: Boolean
) derives Codec.AsObject

case class ClockPlayback(
  id: Option[String],
  job: TrackWrapper,
  when: ClockSchedule,
  enabled: Boolean
) derives Codec.AsObject:
  def toConf = ClockPlaybackConf(id, job.track, when, enabled)

object ClockPlayback:
  def fromConf(conf: ClockPlaybackConf, job: PlaybackJob) =
    apply(conf.id, TrackWrapper(job.trackId), conf.when, conf.enabled)
