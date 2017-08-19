package com.malliina.musicpimp.audio

import rx.lang.scala.{Observable, Subject}

import scala.concurrent.stm.Ref

class PimpPlaylist extends BasePlaylist[PlayableTrack] {
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)
  val songs: Ref[Seq[PlayableTrack]] = Ref[Seq[PlayableTrack]](Nil)

  private val subject = Subject[ServerMessage]().toSerialized
  val events: Observable[ServerMessage] = subject

  protected override def onPlaylistIndexChanged(idx: Int) =
    send(PlaylistIndexChangedMessage(idx))

  protected override def onPlaylistModified(tracks: Seq[PlayableTrack]) =
    send(PlaylistModifiedMessage(tracks))

  def send(json: ServerMessage) = subject.onNext(json)
}
