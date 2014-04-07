package com.mle.musicpimp.library

import com.mle.audio.meta.{UriSource, SongTags, SongMeta}
import models.MusicItemInfo
import com.mle.util.Log
import java.net.URI
import scala.concurrent.duration._
import com.mle.storage.StorageSize
import com.mle.musicpimp.audio.{StoragePlayer, PimpPlayer, PlayableTrack}

/**
 *
 * @author mle
 */
class LocalTrack(id: String, val meta: SongMeta)
  extends MusicItemInfo(meta.tags.title, id, dir = false)
  with PlayableTrack {

  val media = meta.media

  override val size: StorageSize = media.size

  override val duration: Duration = media.duration

  override val album: String = meta.tags.album

  override val artist: String = meta.tags.artist

  override def toString = id

  override def buildPlayer(): PimpPlayer = new StoragePlayer(this)
}

object LocalTrack extends Log {
  val empty = new LocalTrack(
    id = "",
    meta = SongMeta(
      UriSource(new URI("http://www.musicpimp.org/"), 0 seconds, StorageSize.empty),
      tags = new SongTags("", "", "")
    ))
}