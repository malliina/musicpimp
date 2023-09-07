package com.malliina.musicpimp.audio

import java.nio.file.{Path, Paths}
import com.malliina.http.FullUrl
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath
import play.api.libs.json.{Format, Json, OFormat, Reads}

import scala.concurrent.duration.{Duration, FiniteDuration}

case class FullTrack(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  path: UnixPath,
  duration: FiniteDuration,
  size: StorageSize,
  url: FullUrl
) extends TrackMeta

object FullTrack {
  val empty = FullTrack(
    TrackID(""),
    "",
    "",
    "",
    UnixPath.Empty,
    Duration.Zero,
    StorageSize.empty,
    FullUrl("https", "www.musicpimp.org", "")
  )

  implicit val dur: Format[FiniteDuration] = CrossFormats.finiteDuration
  implicit val storage: Format[StorageSize] = CrossFormats.storageSize
  implicit val json: OFormat[FullTrack] = Json.format[FullTrack]
}

trait TrackMeta extends MusicItem {
  def id: TrackID
  def title: String
  def artist: String
  def album: String
  def path: UnixPath
  def duration: FiniteDuration
  def size: StorageSize
  def relativePath: Path = Paths.get(path.path)
  def toTrack = Track(id, title, artist, album, duration, size, path)
  def toFull(url: FullUrl) = FullTrack(id, title, artist, album, path, duration, size, url)
}

object TrackMeta {
  implicit val reader: Reads[TrackMeta] = Track.jsonFormat.map[TrackMeta](identity)
}

case class Track(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  duration: FiniteDuration,
  size: StorageSize,
  path: UnixPath
) extends TrackMeta

object Track {
  implicit val dur: Format[FiniteDuration] = CrossFormats.finiteDuration
  implicit val storage: Format[StorageSize] = CrossFormats.storageSize
  implicit val jsonFormat: OFormat[Track] = Json.format[Track]
}
