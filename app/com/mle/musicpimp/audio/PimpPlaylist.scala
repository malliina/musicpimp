package com.mle.musicpimp.audio

import collection.mutable
import com.mle.util.Log
import com.mle.musicpimp.json.{JsonMessages, JsonSendah}

/**
 * @author Michael
 */
class PimpPlaylist extends BasePlaylist[TrackMeta] with JsonSendah with Log {
  // TODO scala-stm
  val songs = mutable.Buffer.empty[TrackMeta]

  protected override def onPlaylistIndexChanged(idx: Int) =
    send(JsonMessages.playlistIndexChanged(idx))

  protected override def onPlaylistModified(tracks: Seq[TrackMeta]) =
    send(JsonMessages.playlistModified(tracks))
}
