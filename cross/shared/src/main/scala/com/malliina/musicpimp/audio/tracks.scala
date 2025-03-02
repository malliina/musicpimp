package com.malliina.musicpimp.audio

import com.malliina.http.FullUrl
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath

import java.nio.file.{Path, Paths}
import com.malliina.musicpimp.json.CrossFormats.finiteDuration
import io.circe.{Codec, Decoder}

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
) extends TrackMeta derives Codec.AsObject

object FullTrack:
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

trait TrackMeta extends MusicItem:
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

object TrackMeta:
  implicit val reader: Decoder[TrackMeta] = Decoder[Track].map(identity)

case class Track(
  id: TrackID,
  title: String,
  artist: String,
  album: String,
  duration: FiniteDuration,
  size: StorageSize,
  path: UnixPath
) extends TrackMeta derives Codec.AsObject
