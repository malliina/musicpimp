package com.malliina.musicpimp.library

import com.malliina.audio.meta.{SongMeta, StreamSource}
import com.malliina.musicpimp.audio.{PimpPlayer, PlayableTrack, StoragePlayer}
import com.malliina.musicpimp.models.{MusicItem, TrackID}
import com.malliina.storage.StorageSize
import com.malliina.values.UnixPath

import scala.concurrent.duration._

class LocalTrack(val id: TrackID,
                 val path: UnixPath,
                 val meta: SongMeta) extends MusicItem with PlayableTrack {
  val media: StreamSource = meta.media
  override val title = meta.tags.title
  override val size: StorageSize = media.size
  override val duration: Duration = media.duration
  override val album: String = meta.tags.album
  override val artist: String = meta.tags.artist

  override def toString = id.id

  override def buildPlayer(eom: () => Unit): PimpPlayer =
    new StoragePlayer(this, eom)
}
