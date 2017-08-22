package com.malliina.musicpimp.audio

import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.Target
import com.malliina.play.models.Username
import play.api.libs.json.Json._
import play.api.libs.json.{Format, Writes}

import scala.concurrent.stm.Ref

class PimpWebPlaylist(val user: Username, val target: Target)(implicit w: Writes[TrackMeta])
  extends BasePlaylist[TrackMeta]
    with JsonSender {
  implicit val f = Format(TrackMetas.reader, w)
  val songs = Ref[Seq[TrackMeta]](Nil)
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)

  override def add(song: TrackMeta) {
    super.add(song)
    send(obj(Cmd -> toJson(Add), TrackKey -> toJson(song)))
  }

  def notifyPlaylistModified(): Unit =
    sendPayload(PlaylistModifiedMessage(songList))

  override def delete(position: Int) {
    super.delete(position)
    sendCommand(Remove, position)
  }

  override def set(song: TrackMeta) {
    super.set(song)
    send(obj(Cmd -> toJson(Play), TrackKey -> toJson(song), UsernameKey -> user.name))
  }

  override protected def onPlaylistIndexChanged(idx: Int): Unit =
    sendPayload(PlaylistIndexChangedMessage(idx))

  override protected def onPlaylistModified(songs: Seq[TrackMeta]): Unit =
    sendPayload(PlaylistModifiedMessage(songList))
}
