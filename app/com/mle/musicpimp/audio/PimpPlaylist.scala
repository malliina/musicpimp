package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonMessages
import com.mle.util.Log
import play.api.libs.json.JsValue
import rx.lang.scala.{Observable, Subject}

import scala.concurrent.stm.Ref

/**
 * @author Michael
 */
class PimpPlaylist extends BasePlaylist[PlayableTrack] with Log {
  val pos: Ref[PlaylistIndex] = Ref[PlaylistIndex](NO_POSITION)
  val songs: Ref[Seq[PlayableTrack]] = Ref[Seq[PlayableTrack]](Nil)

  private val subject = Subject[JsValue]()
  val events: Observable[JsValue] = subject

  protected override def onPlaylistIndexChanged(idx: Int) =
    send(JsonMessages.playlistIndexChanged(idx))

  protected override def onPlaylistModified(tracks: Seq[PlayableTrack]) =
    send(JsonMessages.playlistModified(tracks))

  def send(json: JsValue) = subject.onNext(json)
}
