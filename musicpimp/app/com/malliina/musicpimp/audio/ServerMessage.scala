package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.http.FullUrl
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.{CrossFormats, JsonMessages}
import com.malliina.musicpimp.json.PlaybackStrings._
import play.api.libs.json._

import scala.concurrent.duration.Duration

sealed trait ServerMessage

case class TrackChangedMessage(track: TrackMeta) extends ServerMessage

case class PlaylistIndexChangedMessage(index: Int) extends ServerMessage

case class PlaylistModifiedMessage(playlist: Seq[TrackMeta]) extends ServerMessage

case class TimeUpdatedMessage(position: Duration) extends ServerMessage

case class PlayStateChangedMessage(state: PlayerStates.Value) extends ServerMessage

case class VolumeChangedMessage(volume: Int) extends ServerMessage

case class MuteToggledMessage(mute: Boolean) extends ServerMessage

object ServerMessage {
  implicit val durFormat = CrossFormats.duration
  val indexChanged = evented(PlaylistIndexChanged, Json.format[PlaylistIndexChangedMessage])
  def playlistModified(implicit w: Format[TrackMeta]) =
    evented(PlaylistModified, Json.format[PlaylistModifiedMessage])
  val timeUpdated = evented(TimeUpdated, Json.format[TimeUpdatedMessage])
  val volumeChanged = evented(VolumeChanged, Json.format[VolumeChangedMessage])
  val muteToggled = evented(MuteToggled, Json.format[MuteToggledMessage])

  def evented[T](eventName: String, payload: OFormat[T]): OFormat[T] = {
    val reader: Reads[T] = Reads { json =>
      (json \ EventKey).validate[String]
        .filter(_ == eventName)
        .flatMap(_ => payload.reads(json))
    }
    val writer = OWrites[T] { t =>
      Json.obj(EventKey -> eventName) ++ payload.writes(t)
    }
    OFormat(reader, writer)
  }

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

  val reader: Reads[ServerMessage] = Reads { json =>
    (json \ EventKey).validate[String].flatMap {
      case PlaylistIndexChanged => indexChanged.reads(json)
    }
  }
}
