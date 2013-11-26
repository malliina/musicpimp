package com.mle.musicpimp.audio

import com.mle.musicpimp.library.TrackInfo
import scala.collection.mutable
import com.mle.musicpimp.json.JsonStrings._
import play.api.libs.json.{JsObject, Json}
import Json._
import com.mle.musicpimp.json.JsonMessages._

/**
 *
 * @author mle
 */
class PimpWebPlaylist(val user: String) extends BasePlaylist[TrackInfo] with JsonSender {
  val songs = mutable.Buffer.empty[TrackInfo]

  override def add(song: TrackInfo) {
    super.add(song)
    send(obj(CMD -> toJson(ADD), TRACK -> toJson(song)))
  }

  def notifyPlaylistModified() {
    send(playlistModified(songList))
  }

  override def delete(position: Int) {
    super.delete(position)
    sendCommand(REMOVE, position)
  }

  override def set(song: TrackInfo) {
    super.set(song)
    send(obj(CMD -> toJson(PLAY), TRACK -> toJson(song)))
  }

  override protected def onPlaylistIndexChanged(idx: Int) {
    send(playlistIndexChanged(idx))
  }

  override protected def onPlaylistModified(songs: Seq[TrackInfo]) {
    send(playlistModified(songList))
  }
}
