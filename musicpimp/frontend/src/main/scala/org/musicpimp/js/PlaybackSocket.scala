package org.musicpimp.js

import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.CrossFormats.durationFormat
import com.malliina.musicpimp.json.PlaybackStrings
import com.malliina.musicpimp.models.SimpleTrack
import play.api.libs.json.{JsError, JsSuccess, JsValue, Reads}

import scala.concurrent.duration.Duration

abstract class PlaybackSocket
  extends SocketJS(Playback.SocketUrl)
    with PlaybackStrings {

  def updateTime(duration: Duration): Unit

  def updatePlayPauseButtons(state: PlayerState): Unit

  def updateTrack(track: SimpleTrack): Unit

  def updatePlaylist(tracks: Seq[SimpleTrack]): Unit

  def updateVolume(vol: Int): Unit

  def muteToggled(isMute: Boolean): Unit

  def onStatus(status: Status): Unit

  override def handlePayload(payload: JsValue): Unit = {
    def read[T: Reads](key: String) = (payload \ key).validate[T]

    val result = read[String](EventKey).flatMap {
      case Welcome =>
        JsSuccess(send(Playback.status))
      case TimeUpdated =>
        read[Duration]("position").map { duration => updateTime(duration) }
      case PlaystateChanged =>
        read[PlayerState]("state").map { state => updatePlayPauseButtons(state) }
      case TrackChanged =>
        read[SimpleTrack]("track").map { track => updateTrack(track) }
      case PlaylistModified =>
        read[Seq[SimpleTrack]]("playlist").map { tracks => updatePlaylist(tracks) }
      case VolumeChanged =>
        read[Int]("volume").map { vol => updateVolume(vol) }
      case MuteToggled =>
        read[Boolean]("mute").map { mute => muteToggled(mute) }
      case Status =>
        payload.validate[Status].map { status => onStatus(status) }
      case PlaylistIndexChanged =>
        JsSuccess(())
      case other =>
        JsError(s"Unknown event '$other'.")
    }
    result.recoverTotal { error => onJsonFailure(error) }
  }
}
