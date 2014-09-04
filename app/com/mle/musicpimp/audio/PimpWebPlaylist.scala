package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonMessages._
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.Json
import play.api.libs.json.Json._

import scala.collection.mutable

/**
 *
 * @author mle
 */
class PimpWebPlaylist(val user: String) extends BasePlaylist[TrackMeta] with JsonSender {
  override val songs = mutable.Buffer.empty[TrackMeta]

  override def add(song: TrackMeta) {
    super.add(song)
    send(obj(CMD -> toJson(ADD), TRACK -> toJson(song)))
  }

  def notifyPlaylistModified() =
    send(playlistModified(songList))

  override def delete(position: Int) {
    super.delete(position)
    sendCommand(REMOVE, position)
  }

  override def set(song: TrackMeta) {
    super.set(song)
    send(obj(CMD -> toJson(PLAY), TRACK -> toJson(song)))
  }

  override protected def onPlaylistIndexChanged(idx: Int) =
    send(playlistIndexChanged(idx))

  override protected def onPlaylistModified(songs: Seq[TrackMeta]) =
    send(playlistModified(songList))
}
