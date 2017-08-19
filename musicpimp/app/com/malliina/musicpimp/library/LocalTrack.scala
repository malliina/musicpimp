package com.malliina.musicpimp.library

import java.net.URI

import com.malliina.audio.meta.{SongMeta, SongTags, UriSource}
import com.malliina.musicpimp.audio.{PimpPlayer, PlayableTrack, StoragePlayer}
import com.malliina.musicpimp.models.{MusicItem, PimpPath, TrackID}
import com.malliina.storage.StorageSize

import scala.concurrent.duration._

class LocalTrack(val id: TrackID,
                 val path: PimpPath,
                 val meta: SongMeta) extends MusicItem with PlayableTrack {
  val media = meta.media
  override val title = meta.tags.title
  override val size: Long = media.size.toBytes
  override val duration: Duration = media.duration
  override val album: String = meta.tags.album
  override val artist: String = meta.tags.artist

  override def toString = id.id

  override def buildPlayer(eom: () => Unit): PimpPlayer = new StoragePlayer(this, eom)
}

object LocalTrack {
  val empty = new LocalTrack(
    id = TrackID(""),
    meta = SongMeta(
      UriSource(new URI("http://www.musicpimp.org/"), 0.seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ),
    path = PimpPath.Empty)
}
