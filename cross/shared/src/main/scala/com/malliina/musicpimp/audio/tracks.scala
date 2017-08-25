package com.malliina.musicpimp.audio

import java.net.URLDecoder

import com.malliina.http.FullUrl
import com.malliina.musicpimp.audio.Enc.UTF8
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath
import play.api.libs.json.{Json, Reads}
import CrossFormats.storageSize
import CrossFormats.duration
import scala.concurrent.duration.Duration

trait TrackMeta extends MusicItem {
  def id: TrackID

  def title: String

  def artist: String

  def album: String

  def path: UnixPath

  def duration: Duration

  def size: StorageSize
}

object TrackMeta {
  implicit val reader: Reads[TrackMeta] = Track.jsonFormat.map[TrackMeta](identity)
}

case class Track(id: TrackID,
                 title: String,
                 artist: String,
                 album: String,
                 duration: Duration,
                 size: StorageSize) extends TrackMeta {
  override def path: UnixPath = UnixPath(URLDecoder.decode(id.id, UTF8))
}

object Track {
//  implicit val dur = CrossFormats.duration
//  implicit val storage = CrossFormats.storageSize
  implicit val jsonFormat = Json.format[Track]
}
