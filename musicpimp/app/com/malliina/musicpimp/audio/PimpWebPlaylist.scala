package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonMessages._
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.play.models.Username
import controllers.musicpimp.{Target, WebPlayer}
import play.api.libs.json.Json._
import play.api.libs.json.Writes

import scala.concurrent.stm.Ref

class PimpWebPlaylist(val user: Username, val target: Target)(implicit w: Writes[TrackMeta])
  extends BasePlaylist[TrackMeta]
    with JsonSender {

  val songs = Ref[Seq[TrackMeta]](Nil)
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)

  override def add(song: TrackMeta) {
    super.add(song)
    send(obj(Cmd -> toJson(Add), TrackKey -> toJson(song)))
  }

  def notifyPlaylistModified() =
    send(playlistModified(songList))

  override def delete(position: Int) {
    super.delete(position)
    sendCommand(Remove, position)
  }

  override def set(song: TrackMeta) {
    super.set(song)
    send(obj(Cmd -> toJson(Play), TrackKey -> toJson(song), UsernameKey -> user.name))
  }

  override protected def onPlaylistIndexChanged(idx: Int) =
    send(playlistIndexChanged(idx))

  override protected def onPlaylistModified(songs: Seq[TrackMeta]) =
    send(playlistModified(songList))
}
