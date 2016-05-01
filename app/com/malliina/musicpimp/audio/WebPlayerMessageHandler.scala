package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.WebPlayerMessageHandler.log
import com.malliina.musicpimp.library.Library
import play.api.Logger
import play.api.libs.json._

import scala.util.Try

trait WebPlayerMessageHandler extends JsonHandlerBase {

  def player(user: String): PimpWebPlayer

  def fulfillMessage(message: PlayerMessage, user: String): Unit = {

    def userPlayer(op: PimpWebPlayer => Unit): Unit =
      op(player(user))

    message match {
      case TimeUpdated(position) =>
        userPlayer(_.position = position)
      case TrackChanged(track) =>
        userPlayer(_.notifyTrackChanged(newTrackInfo(track)))
      case VolumeChanged(volume) =>
        userPlayer(_.notifyVolumeChanged(volume))
      case PlaylistIndexChanged(index) =>
        userPlayer(player => {
          player.playlist.index = index
          player.trackChanged()
        })
      case PlayStateChanged(state) =>
        userPlayer(_.notifyPlayStateChanged(state))
      case MuteToggled(isMute) =>
        userPlayer(_.notifyMuteToggled(isMute))
      case Play(track) =>
        userPlayer(_.setPlaylistAndPlay(newTrackInfo(track)))
      case Add(track) =>
        userPlayer(_.playlist.add(newTrackInfo(track)))
      case Remove(track) =>
        userPlayer(_.playlist.delete(track))
      case Resume =>
        userPlayer(_.play())
      case Stop =>
        userPlayer(_.stop())
      case Next =>
        userPlayer(_.nextTrack())
      case Prev =>
        userPlayer(_.previousTrack())
      case Skip(index) =>
        userPlayer(_.skip(index))
      case Seek(position) =>
        userPlayer(_.seek(position))
      case Mute(isMute) =>
        userPlayer(_.mute(isMute))
      case Volume(volume) =>
        userPlayer(_.gain(1.0f * volume / 100))
      case _ =>
        log warn s"Unsupported message: $message from $user"
    }
  }

  private def newTrackInfo(trackId: String) = Library meta trackId
}

object WebPlayerMessageHandler {
  private val log = Logger(getClass)

  val playerStateReader = Reads[PlayerStates.Value](_.validate[String] flatMap { str =>
    Try(PlayerStates.withName(str))
      .map[JsResult[PlayerStates.Value]](state => JsSuccess(state))
      .getOrElse(JsError(s"Unknown player state: $str"))
  })
}
