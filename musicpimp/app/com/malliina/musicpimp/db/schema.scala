package com.malliina.musicpimp.db

import com.malliina.musicpimp.models.{FolderID, PlaylistID, TrackID}
import com.malliina.values.Username

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

case class PlaylistRecord(id: PlaylistID, name: String, user: Username)
case class PlaylistTotals(id: PlaylistID, tracks: Long, duration: FiniteDuration)
case class PlaylistTrack(playlist: PlaylistID, track: TrackID, idx: Int)
case class PlayPair(track: DataTrack, link: PlaylistTrack)
case class IndexedTrackId(track: TrackID, idx: Int)
case class NewPlaylistInfo(
  id: PlaylistID,
  name: String,
  trackCount: Long,
  duration: FiniteDuration,
  track: Option[IndexedPlaylistTrack]
)

case class PlaylistInfo(
  id: PlaylistID,
  name: String,
  trackCount: Int,
  duration: FiniteDuration,
  track: Option[DataTrack],
  idx: Option[Int]
)
case class PlaybackRecord(track: TrackID, started: Instant, who: Username)
case class TrackRecord(record: PlaybackRecord, track: DataTrack)
case class IndexedPlaylistTrack(playlist: PlaylistID, track: DataTrack, idx: Int)
case class TempFolder(id: FolderID)
case class TempTrack(id: TrackID)
case class IndexResult(totalFiles: Long, foldersPurged: Int, tracksPurged: Int)
