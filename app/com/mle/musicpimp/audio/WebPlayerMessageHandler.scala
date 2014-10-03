package com.mle.musicpimp.audio

import java.util.concurrent.TimeUnit

import com.mle.audio.PlayerStates
import com.mle.musicpimp.json.JsonStrings._
import com.mle.musicpimp.library.Library
import play.api.libs.json.JsValue

import scala.concurrent.duration.{Duration, DurationInt}


/**
 *
 * @author mle
 */
object WebPlayerMessageHandler extends JsonHandlerBase {
  override protected def handleMessage(msg: JsValue, user: String): Unit = {
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

  def webPlayer(user: String, op: PimpWebPlayer => Unit): Unit = WebPlayback.execute(user, op)

  private def newTrackInfo(trackId: String) = Library meta trackId
}