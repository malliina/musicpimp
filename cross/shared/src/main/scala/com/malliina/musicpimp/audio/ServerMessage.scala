package com.malliina.musicpimp.audio

import com.malliina.musicpimp.audio.ServerMessage.evented
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.CrossFormats.duration
import com.malliina.musicpimp.json.PlaybackStrings._
import play.api.libs.json.Json.toJson
import play.api.libs.json._

import scala.concurrent.duration.Duration
import scala.language.implicitConversions

sealed trait ServerMessage

case class TrackChangedMessage(track: TrackMeta) extends ServerMessage

object TrackChangedMessage {
  implicit def json(implicit w: Format[TrackMeta]): OFormat[TrackChangedMessage] =
    evented(TrackChanged, Json.format[TrackChangedMessage])
}

case class PlaylistIndexChangedMessage(index: Int) extends ServerMessage

object PlaylistIndexChangedMessage {
  // legacy support
  private val format = OFormat[PlaylistIndexChangedMessage](
    Reads(json => (json \ PlaylistIndex).validate[Int].map(apply)),
    OWrites[PlaylistIndexChangedMessage](pic => Json.obj(PlaylistIndex -> pic.index, PlaylistIndexv17v18 -> pic.index))
  )
  implicit val json = evented(PlaylistIndexChanged, format)
}

case class PlaylistModifiedMessage(playlist: Seq[TrackMeta]) extends ServerMessage

object PlaylistModifiedMessage {
  implicit def json(implicit w: Format[TrackMeta]): OFormat[PlaylistModifiedMessage] =
    evented(PlaylistModified, Json.format[PlaylistModifiedMessage])
}

case class TimeUpdatedMessage(position: Duration) extends ServerMessage

object TimeUpdatedMessage {
  implicit val json = evented(TimeUpdated, Json.format[TimeUpdatedMessage])
}

case class PlayStateChangedMessage(state: PlayState) extends ServerMessage

object PlayStateChangedMessage {
  implicit val json = evented(PlaystateChanged, Json.format[PlayStateChangedMessage])
}

case class VolumeChangedMessage(volume: Int) extends ServerMessage

object VolumeChangedMessage {
  implicit val json = evented(VolumeChanged, Json.format[VolumeChangedMessage])
}

case class MuteToggledMessage(mute: Boolean) extends ServerMessage

object MuteToggledMessage {
  implicit val json = evented(MuteToggled, Json.format[MuteToggledMessage])
}

object ServerMessage {
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


  implicit def jsonWriter(implicit f: Format[TrackMeta]): Writes[ServerMessage] = {
    Writes[ServerMessage] {
      case tc: TrackChangedMessage =>
        toJson(tc)(TrackChangedMessage.json(f))
      case pm: PlaylistModifiedMessage =>
        toJson(pm)(PlaylistModifiedMessage.json(f))
      case pic: PlaylistIndexChangedMessage =>
        toJson(pic)
      case psc: PlayStateChangedMessage =>
        toJson(psc)
      case mt: MuteToggledMessage =>
        toJson(mt)
      case vc: VolumeChangedMessage =>
        toJson(vc)
      case tu: TimeUpdatedMessage =>
        toJson(tu)
    }
  }

  implicit val reader: Reads[ServerMessage] = Reads { json =>
    implicit val trackFormat = JsonHelpers.dummyTrackFormat
    json.validate[TrackChangedMessage]
      .orElse(json.validate[PlaylistModifiedMessage])
      .orElse(json.validate[PlaylistIndexChangedMessage])
      .orElse(json.validate[PlayStateChangedMessage])
      .orElse(json.validate[MuteToggledMessage])
      .orElse(json.validate[VolumeChangedMessage])
      .orElse(json.validate[TimeUpdatedMessage])
  }
}
