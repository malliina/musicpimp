package com.mle.musicpimp.audio

import collection.mutable
import com.mle.util.Log
import com.mle.musicpimp.library.TrackInfo
import com.mle.musicpimp.json.{JsonMessages, JsonSendah}

/**
 * @author Michael
 */
class PimpPlaylist extends BasePlaylist[TrackInfo] with JsonSendah with Log {
  val songs = mutable.Buffer.empty[TrackInfo]

  protected override def onPlaylistIndexChanged(idx: Int) {
    send(JsonMessages.playlistIndexChanged(idx))
  }

  protected override def onPlaylistModified(tracks: Seq[TrackInfo]) {
    send(JsonMessages.playlistModified(tracks))
  }
}
