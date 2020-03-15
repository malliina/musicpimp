package com.malliina.musicpimp.audio

import akka.stream.Materializer
import com.malliina.streams.StreamsUtil

import scala.concurrent.stm.Ref

class PimpPlaylist()(implicit mat: Materializer)
  extends BasePlaylist[PlayableTrack]
  with AutoCloseable {
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)
  val songs: Ref[Seq[PlayableTrack]] = Ref[Seq[PlayableTrack]](Nil)

  private val eventHub = StreamsUtil.connectedStream[ServerMessage]()
  val events = eventHub.source

  protected override def onPlaylistIndexChanged(idx: Int): Unit =
    send(PlaylistIndexChangedMessage(idx))

  protected override def onPlaylistModified(tracks: Seq[PlayableTrack]): Unit =
    send(PlaylistModifiedMessage(tracks))

  def send(json: ServerMessage): Unit =
    eventHub.send(json)

  def close(): Unit = eventHub.shutdown()
}
