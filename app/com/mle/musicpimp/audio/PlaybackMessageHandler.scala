package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.library.Library
import play.api.libs.json.JsValue

import scala.concurrent.duration.DurationDouble
import scala.util.Try

/**
 *
 * @author mle
 */
object PlaybackMessageHandler extends JsonHandlerBase {
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
        MusicPlayer.seek(pos.toDouble seconds)
      case PLAY =>
        val track = cmd.track
        //        log.info(s"Resetting library with track: $track")
        MusicPlayer.reset(Library meta track)
      //        log.info("Reset done")
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
      case anythingElse =>
        log error s"Invalid JSON: $msg"
    })
  }
}