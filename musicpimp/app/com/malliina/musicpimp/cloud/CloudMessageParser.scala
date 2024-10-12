package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.audio.PlayerMessage
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudStrings.Registered
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.JsonStrings.*
import com.malliina.musicpimp.json.SocketStrings.Cancel
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{PlaylistID, RequestID}
import com.malliina.musicpimp.scheduler.json.AlarmCommand
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.pimpcloud.SharedStrings.{Ping, Pong}
import com.malliina.play.json.JsonStrings.Cmd
import com.malliina.values.Username
import io.circe.{Decoder, DecodingFailure, Json}

object CloudMessageParser extends CloudMessageParser

trait CloudMessageParser:
  def parseRequest(json: Json): Decoder.Result[CloudRequest] =
    val cursor = json.hcursor
    val cmd = cursor.downField(Cmd).as[String]
    val request = cursor.downField(CloudResponse.RequestKey).as[RequestID]
    val user = cursor.downField(UsernameKey).as[Username]
    val body = cursor.downField(Body)

    def withUser[T](transform: Username => Decoder.Result[T]): Decoder.Result[T] =
      user.flatMap(transform)

    def readMeta: Decoder.Result[DataRequest] = for
      u <- user
      b <- body.as[Json]
      meta <- DataRequest.fromJson(u, b)
    yield meta

    request flatMap { req =>
      val message: Decoder.Result[PimpMessage] = cmd flatMap {
        case VersionKey      => Right(GetVersion)
        case TrackKey        => body.as[RangedTrack] orElse body.as[GetTrack]
        case Cancel          => Right(CancelStream(req))
        case Meta            => body.as[GetMeta]
        case Ping            => Right(PingMessage)
        case AuthenticateKey => body.as[Authenticate]
        case RootFolderKey   => Right(RootFolder)
        case FolderKey       => body.as[GetFolder]
        case SearchKey       => body.as[Search]
        case PlaylistsGet    => user.map(u => GetPlaylists(u))
        case PlaylistGet => withUser(u => body.downField(Id).as[PlaylistID].map(GetPlaylist(_, u)))
        case PlaylistSave =>
          withUser(u => body.downField(PlaylistKey).as[PlaylistSubmission].map(SavePlaylist(_, u)))
        case PlaylistDelete =>
          withUser(u => body.downField(Id).as[PlaylistID].map(DeletePlaylist(_, u)))
        case AlarmsKey  => Right(GetAlarms)
        case AlarmsEdit => body.as[AlarmCommand].map(AlarmEdit.apply)
        case AlarmsAdd  => body.as[AlarmCommand].map(AlarmAdd.apply)
        case Beam       => body.as[BeamCommand]
        case StatusKey  => Right(GetStatus)
        case Recent     => readMeta.map(GetRecent.apply)
        case Popular    => readMeta.map(GetPopular.apply)
        case other      => Left(DecodingFailure(s"Unknown JSON command: '$other' in '$json'.", Nil))
      }
      message.map(msg => CloudRequest(msg, req))
    }

  def parseEvent(json: Json): Decoder.Result[PimpMessage] =
    val cursor = json.hcursor
    val event = cursor.downField(Cmd).as[String].orElse(cursor.downField(EventKey).as[String])
    val body = cursor.downField(Body)
    event.flatMap:
      case Registered =>
        body.as[RegisteredMessage]
      case Player =>
        for
          b <- body.as[PlayerMessage]
          user <- cursor.downField(UsernameKey).as[Username]
        yield PlaybackMessage(b, user)
      case Ping =>
        Right(PingMessage)
      case Pong =>
        Right(PongMessage)
      case other =>
        Left(DecodingFailure(s"Unknown JSON event: '$other' in '$json'.", Nil))
