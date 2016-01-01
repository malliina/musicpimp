package com.malliina.musicpimp.library

import java.net.URI

import com.malliina.audio.meta.{SongMeta, SongTags, UriSource}
import com.malliina.musicpimp.audio.{PimpPlayer, PlayableTrack, StoragePlayer}
import com.malliina.musicpimp.models.MusicItem
import com.malliina.storage.StorageSize
import com.malliina.util.Log

import scala.concurrent.duration._

/**
 *
 * @author mle
 */
class LocalTrack(val id: String, val meta: SongMeta) extends MusicItem with PlayableTrack {

  val media = meta.media
  override val title = meta.tags.title

  override val size: StorageSize = media.size

  override val duration: Duration = media.duration

  override val album: String = meta.tags.album

  override val artist: String = meta.tags.artist

  override def toString = id

  override def buildPlayer(eom: () => Unit): PimpPlayer = new StoragePlayer(this, eom)
}

object LocalTrack extends Log {
  val empty = new LocalTrack(
    id = "",
    meta = SongMeta(
      UriSource(new URI("http://www.musicpimp.org/"), 0.seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ))
}
