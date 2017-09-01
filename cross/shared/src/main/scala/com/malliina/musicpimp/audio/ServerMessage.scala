package com.malliina.musicpimp.audio

import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.CrossFormats.{duration, evented}
import com.malliina.musicpimp.json.PlaybackStrings._
import com.malliina.musicpimp.models.Volume
import play.api.libs.json._

import scala.concurrent.duration.Duration
import scala.language.implicitConversions

/** Server-side playback events sent to clients so that they can update their UI accordingly.
  */
sealed trait ServerMessage

case object PingEvent {
  val Key = "ping"
  implicit val json = evented(Key, CrossFormats.pure(PingEvent))
}

case object WelcomeMessage extends ServerMessage {
  implicit val json = evented(Welcome, CrossFormats.pure(WelcomeMessage))
}

case class StatusMessage(status: StatusEvent) extends ServerMessage

object StatusMessage {
  implicit val reader = Reads[StatusMessage] { json =>
    (json \ EventKey).validate[String].filter(_ == Status).flatMap { _ =>
      json.validate[StatusEvent].map(apply)
    }
  }

  implicit def writer(implicit w: Writes[TrackMeta]): OWrites[StatusMessage] =
    OWrites[StatusMessage] { s => Json.obj(EventKey -> Status) ++ StatusEvent.status18writer.writes(s.status) }

  implicit def json(implicit w: Writes[TrackMeta]): OFormat[StatusMessage] =
    OFormat(reader, writer)
}

case class TrackChangedMessage(track: TrackMeta) extends ServerMessage

object TrackChangedMessage {
  implicit def json(implicit w: Writes[TrackMeta]): OFormat[TrackChangedMessage] =
    evented(TrackChanged, Json.format[TrackChangedMessage])
}

case class PlaylistIndexChangedMessage(index: Int) extends ServerMessage

object PlaylistIndexChangedMessage {
  // legacy support
  private val writer = OWrites[PlaylistIndexChangedMessage] { pic =>
    Json.obj(
      PlaylistIndex -> pic.index,
      PlaylistIndexv17v18 -> pic.index)
  }
  private val format = OFormat[PlaylistIndexChangedMessage](
    Reads(json => (json \ PlaylistIndex).validate[Int].map(apply)),
    writer
  )
  implicit val json = evented(PlaylistIndexChanged, format)
}

case class PlaylistModifiedMessage(playlist: Seq[TrackMeta]) extends ServerMessage

object PlaylistModifiedMessage {
  implicit def json(implicit w: Writes[TrackMeta]): OFormat[PlaylistModifiedMessage] =
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

case class VolumeChangedMessage(volume: Volume) extends ServerMessage

object VolumeChangedMessage {
  implicit val json = evented(VolumeChanged, Json.format[VolumeChangedMessage])
}

case class MuteToggledMessage(mute: Boolean) extends ServerMessage

object MuteToggledMessage {
  implicit val json = evented(MuteToggled, Json.format[MuteToggledMessage])
}

object ServerMessage {
  implicit def jsonWriter(implicit f: Writes[TrackMeta]): Writes[ServerMessage] = {
    Writes[ServerMessage] {
      case tc: TrackChangedMessage =>
        TrackChangedMessage.json.writes(tc)
      case pm: PlaylistModifiedMessage =>
        PlaylistModifiedMessage.json(f).writes(pm)
      case pic: PlaylistIndexChangedMessage =>
        PlaylistIndexChangedMessage.json.writes(pic)
      case psc: PlayStateChangedMessage =>
        PlayStateChangedMessage.json.writes(psc)
      case mt: MuteToggledMessage =>
        MuteToggledMessage.json.writes(mt)
      case vc: VolumeChangedMessage =>
        VolumeChangedMessage.json.writes(vc)
      case tu: TimeUpdatedMessage =>
        TimeUpdatedMessage.json.writes(tu)
      case WelcomeMessage =>
        WelcomeMessage.json.writes(WelcomeMessage)
      case s: StatusMessage =>
        StatusMessage.json.writes(s)
      case _ =>
        // TODO ???
        Json.obj()
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
      .orElse(WelcomeMessage.json.reads(json))
      .orElse(StatusMessage.json.reads(json))
  }
}
