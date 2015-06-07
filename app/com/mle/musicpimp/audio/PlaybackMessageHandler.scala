package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.library.{LocalTrack, Library}
import play.api.libs.json.JsValue

import scala.concurrent.duration.DurationDouble
import scala.util.Try

/**
 *
 * @author mle
 */
object PlaybackMessageHandler extends JsonHandlerBase {
  def handleMessage(msg: JsValue): Unit = handleMessage(msg, "")

  override protected def handleMessage(msg: JsValue, user: String): Unit = {
    withCmd(msg)(cmd => cmd.command match {
      case RESUME =>
        MusicPlayer.play()
      case STOP =>
        MusicPlayer.stop()
      case NEXT =>
        MusicPlayer.nextTrack()
      case PREV =>
        MusicPlayer.previousTrack()
      case MUTE =>
        MusicPlayer.mute(cmd.boolValue)
      case VOLUME =>
        val vol = cmd.value
        MusicPlayer.volume(vol)
      case SEEK =>
        val pos = cmd.value
        MusicPlayer.seek(pos.toDouble.seconds)
      case PLAY =>
        val track = cmd.track
        MusicPlayer.reset(Library meta track)
      case SKIP =>
        val index = cmd.value
        Try(MusicPlayer skip index).recover {
          case iae: IllegalArgumentException => log.warn(s"Cannot skip to index $index. Reason: ${iae.getMessage}")
        }
      case ADD =>
        val track = cmd.track
        MusicPlayer.playlist.add(Library meta track)
      case REMOVE =>
        MusicPlayer.playlist delete cmd.value
      case ADD_ITEMS =>
        withTracks(cmd, ts => ts.foreach(MusicPlayer.playlist.add))
      case PLAY_ITEMS =>
        withTracks(cmd, ts => {
          if(ts.nonEmpty) {
            MusicPlayer.reset(ts.head)
            ts.tail.foreach(track => MusicPlayer.playlist.add(track))
          }
        })
      case anythingElse =>
        log error s"Invalid JSON: $msg"
    })
    def withTracks(cmd: JsonCmd, f: Seq[LocalTrack] => Unit): Unit = {
      val folders = cmd.foldersOrNil
      val tracks = cmd.tracksOrNil
      val allTracks: Seq[LocalTrack] = folders.flatMap(Library.localTracksIn(_).getOrElse(Nil)) ++ tracks.map(Library.meta)
      f(allTracks)
    }
  }
}