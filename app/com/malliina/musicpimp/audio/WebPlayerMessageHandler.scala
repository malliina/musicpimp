package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.WebPlayerMessageHandler.log
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{RemoteInfo, TrackID}
import play.api.Logger
import play.api.libs.json._

import scala.util.Try

trait WebPlayerMessageHandler extends JsonHandlerBase {

  def player(request: RemoteInfo): PimpWebPlayer

  def fulfillMessage(message: PlayerMessage, request: RemoteInfo): Unit = {

    def userPlayer(op: PimpWebPlayer => Unit): Unit =
      op(player(request))

    message match {
      case TimeUpdatedMsg(position) =>
        userPlayer(_.position = position)
      case TrackChangedMsg(track) =>
        userPlayer(_.notifyTrackChanged(newTrackInfo(track)))
      case VolumeChangedMsg(volume) =>
        userPlayer(_.notifyVolumeChanged(volume))
      case PlaylistIndexChangedMsg(index) =>
        userPlayer(player => {
          player.playlist.index = index
          player.trackChanged()
        })
      case PlayStateChangedMsg(state) =>
        userPlayer(_.notifyPlayStateChanged(state))
      case MuteToggledMsg(isMute) =>
        userPlayer(_.notifyMuteToggled(isMute))
      case PlayMsg(track) =>
        userPlayer(_.setPlaylistAndPlay(newTrackInfo(track)))
      case AddMsg(track) =>
        userPlayer(_.playlist.add(newTrackInfo(track)))
      case RemoveMsg(track) =>
        userPlayer(_.playlist.delete(track))
      case ResumeMsg =>
        userPlayer(_.play())
      case StopMsg =>
        userPlayer(_.stop())
      case NextMsg =>
        userPlayer(_.nextTrack())
      case PrevMsg =>
        userPlayer(_.previousTrack())
      case SkipMsg(index) =>
        userPlayer(_.skip(index))
      case SeekMsg(position) =>
        userPlayer(_.seek(position))
      case MuteMsg(isMute) =>
        userPlayer(_.mute(isMute))
      case VolumeMsg(volume) =>
        userPlayer(_.gain(1.0f * volume / 100))
      case _ =>
        log warn s"Unsupported message: $message from ${request.user}"
    }
  }

  private def newTrackInfo(trackId: TrackID) = Library meta trackId
}

object WebPlayerMessageHandler {
  private val log = Logger(getClass)

  val playerStateReader = Reads[PlayerStates.Value](_.validate[String] flatMap { str =>
    Try(PlayerStates.withName(str))
      .map[JsResult[PlayerStates.Value]](state => JsSuccess(state))
      .getOrElse(JsError(s"Unknown player state: $str"))
  })
}
