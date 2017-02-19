package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.json.JsonMessages
import com.malliina.musicpimp.models.FullUrl
import play.api.libs.json.Writes

import scala.concurrent.duration.Duration

sealed trait ServerMessage

object ServerMessage {
  def writer(host: FullUrl): Writes[ServerMessage] = {
    implicit val tw = TrackJson.writer(host)
    Writes[ServerMessage] {
      case TrackChangedMessage(track) =>
        JsonMessages.trackChanged(track)
      case PlaylistModifiedMessage(ts) =>
        JsonMessages.playlistModified(ts)
      case PlaylistIndexChangedMessage(idx) =>
        JsonMessages.playlistIndexChanged(idx)
      case PlayStateChangedMessage(state) =>
        JsonMessages.playStateChanged(state)
      case MuteToggledMessage(mute) =>
        JsonMessages.muteToggled(mute)
      case VolumeChangedMessage(volume) =>
        JsonMessages.volumeChanged(volume)
      case TimeUpdatedMessage(time) =>
        JsonMessages.timeUpdated(time)
    }
  }

  case class TrackChangedMessage(track: TrackMeta) extends ServerMessage

  case class PlaylistIndexChangedMessage(index: Int) extends ServerMessage

  case class PlaylistModifiedMessage(tracks: Seq[TrackMeta]) extends ServerMessage

  case class TimeUpdatedMessage(position: Duration) extends ServerMessage

  case class PlayStateChangedMessage(state: PlayerStates.Value) extends ServerMessage

  case class VolumeChangedMessage(volume: Int) extends ServerMessage

  case class MuteToggledMessage(mute: Boolean) extends ServerMessage

}
