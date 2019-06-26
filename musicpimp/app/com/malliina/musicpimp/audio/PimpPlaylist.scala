package com.malliina.musicpimp.audio

import akka.stream.Materializer
import com.malliina.rx.Sources

import scala.concurrent.stm.Ref

class PimpPlaylist()(implicit mat: Materializer) extends BasePlaylist[PlayableTrack] {
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)
  val songs: Ref[Seq[PlayableTrack]] = Ref[Seq[PlayableTrack]](Nil)

  private val (eventsTarget, eventSource) = Sources.connected[ServerMessage]
  val events = eventSource

  protected override def onPlaylistIndexChanged(idx: Int): Unit =
    send(PlaylistIndexChangedMessage(idx))

  protected override def onPlaylistModified(tracks: Seq[PlayableTrack]): Unit =
    send(PlaylistModifiedMessage(tracks))

  def send(json: ServerMessage): Unit = eventsTarget ! json
}
