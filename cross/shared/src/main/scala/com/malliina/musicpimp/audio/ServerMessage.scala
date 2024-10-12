package com.malliina.musicpimp.audio

import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.CrossFormats
import com.malliina.musicpimp.json.CrossFormats.{duration, evented}
import com.malliina.musicpimp.json.PlaybackStrings.*
import com.malliina.musicpimp.models.Volume
import io.circe.generic.semiauto.deriveCodec
import io.circe.syntax.EncoderOps
import io.circe.{Codec, Decoder, DecodingFailure, Encoder, Json}

import scala.concurrent.duration.Duration
import scala.language.implicitConversions

/** Server-side playback events sent to clients so that they can update their UI accordingly.
  */
sealed trait ServerMessage

case object PingEvent extends ServerMessage:
  val Key = "ping"
  implicit val json: Codec[PingEvent.type] = evented(Key, CrossFormats.pure(PingEvent))

case object WelcomeMessage extends ServerMessage:
  implicit val json: Codec[WelcomeMessage.type] =
    evented(Welcome, CrossFormats.pure(WelcomeMessage))

case class StatusMessage(status: StatusEvent) extends ServerMessage

object StatusMessage:
  implicit val reader: Decoder[StatusMessage] = Decoder[StatusMessage]: json =>
    json
      .downField(EventKey)
      .as[String]
      .flatMap: s =>
        if s == Status then json.as[StatusEvent].map(apply)
        else Left(DecodingFailure("Not status", Nil))

  implicit def writer: Encoder[StatusMessage] =
    (s: StatusMessage) => Json.obj(EventKey -> Status.asJson).deepMerge(StatusEvent.json(s.status))

  implicit def json(implicit w: Encoder[TrackMeta]): Codec[StatusMessage] =
    Codec.from(reader, writer)

case class TrackChangedMessage(track: TrackMeta) extends ServerMessage

object TrackChangedMessage:
  implicit def json(implicit w: Encoder[TrackMeta]): Codec[TrackChangedMessage] =
    evented(TrackChanged, deriveCodec[TrackChangedMessage])

case class PlaylistIndexChangedMessage(index: Int) extends ServerMessage

object PlaylistIndexChangedMessage:
  // legacy support
  private val writer = Encoder[PlaylistIndexChangedMessage]: pic =>
    Json.obj(PlaylistIndex -> pic.index.asJson, PlaylistIndexv17v18 -> pic.index.asJson)
  private val format = Codec.from[PlaylistIndexChangedMessage](
    Decoder[PlaylistIndexChangedMessage]: json =>
      json.downField(PlaylistIndex).as[Int].map(apply),
    writer
  )
  implicit val json: Codec[PlaylistIndexChangedMessage] = evented(PlaylistIndexChanged, format)

case class PlaylistModifiedMessage(playlist: Seq[TrackMeta]) extends ServerMessage

object PlaylistModifiedMessage:
  implicit def json(implicit w: Encoder[TrackMeta]): Codec[PlaylistModifiedMessage] =
    evented(PlaylistModified, deriveCodec[PlaylistModifiedMessage])

case class TimeUpdatedMessage(position: Duration) extends ServerMessage

object TimeUpdatedMessage:
  implicit val json: Codec[TimeUpdatedMessage] =
    evented(TimeUpdated, deriveCodec[TimeUpdatedMessage])

case class PlayStateChangedMessage(state: PlayState) extends ServerMessage

object PlayStateChangedMessage:
  implicit val json: Codec[PlayStateChangedMessage] =
    evented(PlaystateChanged, deriveCodec[PlayStateChangedMessage])

case class VolumeChangedMessage(volume: Volume) extends ServerMessage

object VolumeChangedMessage:
  implicit val json: Codec[VolumeChangedMessage] =
    evented(VolumeChanged, deriveCodec[VolumeChangedMessage])

case class MuteToggledMessage(mute: Boolean) extends ServerMessage

object MuteToggledMessage:
  implicit val json: Codec[MuteToggledMessage] =
    evented(MuteToggled, deriveCodec[MuteToggledMessage])

object ServerMessage:
  implicit def jsonWriter(implicit f: Encoder[TrackMeta]): Encoder[ServerMessage] =
    Encoder[ServerMessage]:
      case tc: TrackChangedMessage =>
        TrackChangedMessage.json(f)(tc)
      case pm: PlaylistModifiedMessage =>
        PlaylistModifiedMessage.json(f)(pm)
      case pic: PlaylistIndexChangedMessage =>
        PlaylistIndexChangedMessage.json(pic)
      case psc: PlayStateChangedMessage =>
        PlayStateChangedMessage.json(psc)
      case mt: MuteToggledMessage =>
        MuteToggledMessage.json(mt)
      case vc: VolumeChangedMessage =>
        VolumeChangedMessage.json(vc)
      case tu: TimeUpdatedMessage =>
        TimeUpdatedMessage.json(tu)
      case WelcomeMessage =>
        WelcomeMessage.json(WelcomeMessage)
      case s: StatusMessage =>
        StatusMessage.json(f)(s)
      case PingEvent =>
        PingEvent.json(PingEvent)

  implicit val reader: Decoder[ServerMessage] = Decoder[ServerMessage]: json =>
    implicit val trackFormat: Codec[TrackMeta] = JsonHelpers.dummyTrackFormat
    json
      .as[TrackChangedMessage]
      .orElse(json.as[PlaylistModifiedMessage])
      .orElse(json.as[PlaylistIndexChangedMessage])
      .orElse(json.as[PlayStateChangedMessage])
      .orElse(json.as[MuteToggledMessage])
      .orElse(json.as[VolumeChangedMessage])
      .orElse(json.as[TimeUpdatedMessage])
      .orElse(WelcomeMessage.json.decodeJson(json.value))
      .orElse(StatusMessage.json.decodeJson(json.value))
      .orElse(PingEvent.json.decodeJson(json.value))
