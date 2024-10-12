package org.musicpimp.js

import com.malliina.musicpimp.audio.*
import com.malliina.musicpimp.json.CrossFormats.duration
import com.malliina.musicpimp.json.PlaybackStrings
import com.malliina.musicpimp.models.Volume
import io.circe.Json

import scala.concurrent.duration.Duration

abstract class PlaybackSocket extends SocketJS(Playback.SocketUrl) with PlaybackStrings:

  def updateTime(duration: Duration): Unit

  def updatePlayPauseButtons(state: PlayState): Unit

  def updateTrack(track: TrackMeta): Unit

  def updatePlaylist(tracks: Seq[TrackMeta]): Unit

  def updateVolume(vol: Volume): Unit

  def muteToggled(isMute: Boolean): Unit

  def onStatus(status: StatusEvent): Unit

  override def handlePayload(payload: Json): Unit =
    payload
      .as[ServerMessage]
      .map(handleMessage)
      .left
      .map: error =>
        onJsonFailure(error.message)

  def handleMessage(message: ServerMessage): Unit =
    message match
      case WelcomeMessage                    => send(StatusMsg)
      case StatusMessage(status)             => onStatus(status)
      case TimeUpdatedMessage(position)      => updateTime(position)
      case PlayStateChangedMessage(state)    => updatePlayPauseButtons(state)
      case TrackChangedMessage(track)        => updateTrack(track)
      case PlaylistModifiedMessage(playlist) => updatePlaylist(playlist)
      case VolumeChangedMessage(volume)      => updateVolume(volume)
      case MuteToggledMessage(mute)          => muteToggled(mute)
      case PlaylistIndexChangedMessage(_)    => ()
      case other                             => log.info(s"Not handling '$other'.")
