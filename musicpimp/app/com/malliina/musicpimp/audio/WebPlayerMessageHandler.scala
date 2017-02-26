package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.WebPlayerMessageHandler.log
import com.malliina.musicpimp.library.Library
import com.malliina.musicpimp.models.{RemoteInfo, TrackID}
import play.api.Logger
import play.api.libs.json._

import scala.util.Try

class WebPlayerMessageHandler(request: RemoteInfo, player: PimpWebPlayer)
  extends JsonHandlerBase {
  def fulfillMessage(message: PlayerMessage, request: RemoteInfo): Unit = {
    message match {
      case TimeUpdatedMsg(position) =>
        player.position = position
      case TrackChangedMsg(track) =>
        player.notifyTrackChanged(newTrackInfo(track))
      case VolumeChangedMsg(volume) =>
        player.notifyVolumeChanged(volume)
      case PlaylistIndexChangedMsg(index) =>
        player.playlist.index = index
        player.trackChanged()
      case PlayStateChangedMsg(state) =>
        player.notifyPlayStateChanged(state)
      case MuteToggledMsg(isMute) =>
        player.notifyMuteToggled(isMute)
      case PlayMsg(track) =>
        player.setPlaylistAndPlay(newTrackInfo(track))
      case AddMsg(track) =>
        player.playlist.add(newTrackInfo(track))
      case RemoveMsg(track) =>
        player.playlist.delete(track)
      case ResumeMsg =>
        player.play()
      case StopMsg =>
        player.stop()
      case NextMsg =>
        player.nextTrack()
      case PrevMsg =>
        player.previousTrack()
      case SkipMsg(index) =>
        player.skip(index)
      case SeekMsg(position) =>
        player.seek(position)
      case MuteMsg(isMute) =>
        player.mute(isMute)
      case VolumeMsg(volume) =>
        player.gain(1.0f * volume / 100)
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
