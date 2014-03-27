package com.mle.musicpimp.audio

import com.mle.musicpimp.json.JsonStrings
import play.api.libs.json.JsValue
import com.mle.musicpimp.library.Library
import com.mle.util.Log
import JsonStrings._
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import com.mle.audio.PlayerStates

/**
 *
 * @author Michael
 */

trait JsonMessageHandler extends Log {
  def withCmd[T](json: JsValue)(f: JsonCmd => T): T =
    f(new JsonCmd(json))

  /**
   * Handles messages sent by web players.
   *
   */
  def onClientMessage(user: String, msg: JsValue) {
    def userPlayer(op: PimpWebPlayer => Unit) {
      webPlayer(user, op)
    }
    withCmd(msg)(cmd => cmd.command match {
      case TIME_UPDATED =>
        userPlayer(_.position = cmd.value.seconds)
      case TRACK_CHANGED =>
        userPlayer(_.notifyTrackChanged(Library meta cmd.track))
      case VOLUME_CHANGED =>
        userPlayer(_.notifyVolumeChanged(cmd.value))
      case PLAYLIST_INDEX_CHANGED =>
        userPlayer(player => {
          player.playlist.index = cmd.value
          player.trackChanged()
        })
      case PLAYSTATE_CHANGED =>
        userPlayer(_.notifyPlayStateChanged(PlayerStates.withName(cmd.stringValue)))
      case MUTE_TOGGLED =>
        userPlayer(_.notifyMuteToggled(cmd.boolValue))
      case PLAY =>
        userPlayer(_.setPlaylistAndPlay(newTrackInfo(cmd.track)))
      case ADD =>
        userPlayer(_.playlist.add(newTrackInfo(cmd.track)))
      case REMOVE =>
        userPlayer(_.playlist.delete(cmd.value))
      case RESUME =>
        userPlayer(_.play())
      case STOP =>
        userPlayer(_.stop())
      case NEXT =>
        userPlayer(_.nextTrack())
      case PREV =>
        userPlayer(_.previousTrack())
      case SKIP =>
        userPlayer(_.skip(cmd.value))
      case SEEK =>
        userPlayer(_.seek(Duration(cmd.value, TimeUnit.SECONDS)))
      case MUTE =>
        userPlayer(_.mute(cmd.boolValue))
      case VOLUME =>
        userPlayer(_.gain(1.0f * cmd.value / 100))
      case anythingElse =>
        log warn s"Unknown message: $msg"
    })
  }

  def onPlaybackCommand(jsonCmd: JsValue): Unit = {
    log debug s"Got message: $jsonCmd"
    withCmd(jsonCmd)(cmd => cmd.command match {
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
        MusicPlayer.reset(Library meta track)
      case SKIP =>
        MusicPlayer skip cmd.value
      case ADD =>
        val track = cmd.track
        MusicPlayer.playlist.add(Library meta track)
      case REMOVE =>
        MusicPlayer.playlist delete cmd.value
      case anythingElse =>
        log error s"Invalid JSON: $jsonCmd"
    })
  }

  def webPlayer(user: String, op: PimpWebPlayer => Unit) {
    WebPlayback.execute(user, op)
  }

  private def newTrackInfo(trackId: String) =
    Library meta trackId
}

object JsonMessageHandler extends JsonMessageHandler
