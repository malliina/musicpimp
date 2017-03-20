package org.musicpimp.js

import com.malliina.musicpimp.json.PlaybackStrings
import upickle.{Invalid, Js}

import scala.concurrent.duration.Duration

case class Volume private(volume: Int)

object Volume {
  def forInt(v: Int): Option[Volume] =
    if(v >= 0 && v <= 100) Option(Volume(v))
    else None

  def unapply(json: Js.Value): Option[Volume] = json match {
    case Js.Num(n) => forInt(n.toInt)
    case _ =>  None
  }
}

sealed trait PlayerState

object PlayerState {

  case object Started extends PlayerState

  case object Stopped extends PlayerState

  case object NoMedia extends PlayerState

  case class Unknown(name: String) extends PlayerState

  def forString(s: String) = s match {
    case "Started" => Started
    case "Stopped" => Stopped
    case "NoMedia" => NoMedia
    case other => Unknown(other)
  }
}

abstract class PlaybackSocket extends SocketJS(Playback.SocketUrl) with PlaybackStrings {
  def updateTime(duration: Duration): Unit

  def updatePlayPauseButtons(state: PlayerState)

  def updateTrack(track: Track)

  def updatePlaylist(tracks: Seq[Track])

  def updateVolume(vol: Int)

  def muteToggled(isMute: Boolean)

  def onStatus(status: Status)

  override def handlePayload(payload: Js.Value) = withFailure {
    val obj = payload.obj

    def read[T: PimpJSON.Reader](key: String) = obj.get(key)
      .map(json => PimpJSON.readJs[T](json))
      .getOrElse(throw Invalid.Data(payload, s"Missing key '$key'."))

    read[String]("event") match {
      case "welcome" =>
        send(Playback.status)
      case TimeUpdated =>
        updateTime(read[Duration]("position"))
      case PlaystateChanged =>
        updatePlayPauseButtons(read[PlayerState]("state"))
      case TrackChanged =>
        updateTrack(read[Track]("track"))
      case PlaylistModified =>
        updatePlaylist(read[Seq[Track]]("playlist"))
      case VolumeChanged =>
        updateVolume(read[Int]("volume"))
      case MuteToggled =>
        muteToggled(read[Boolean]("mute"))
      case "status" =>
        onStatus(PimpJSON.readJs[Status](payload))
      case PlaylistIndexChanged =>

      case other =>
        log.info(s"Unknown event '$other'.")
    }
  }

  def withFailure(code: => Any) =
    try {
      code
    } catch {
      case i: Invalid => onJsonFailure(i)
    }
}
