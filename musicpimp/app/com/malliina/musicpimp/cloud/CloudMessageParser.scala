package com.malliina.musicpimp.cloud

import com.malliina.musicpimp.audio.PlayerMessage
import com.malliina.musicpimp.beam.BeamCommand
import com.malliina.musicpimp.cloud.CloudStrings.Registered
import com.malliina.musicpimp.js.FrontStrings.EventKey
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.json.SocketStrings.Cancel
import com.malliina.musicpimp.library.PlaylistSubmission
import com.malliina.musicpimp.models.{PlaylistID, RequestID}
import com.malliina.musicpimp.scheduler.json.AlarmCommand
import com.malliina.musicpimp.stats.DataRequest
import com.malliina.play.json.JsonStrings.Cmd
import com.malliina.values.Username
import play.api.libs.json._

object CloudMessageParser extends CloudMessageParser

trait CloudMessageParser {
  def parseRequest(json: JsValue): JsResult[CloudRequest] = {
    val cmd = (json \ Cmd).validate[String]
    val request = (json \ CloudResponse.RequestKey).validate[RequestID]
    val user = (json \ UsernameKey).validate[Username]
    val body = json \ Body

    def withUser[T](transform: Username => JsResult[T]): JsResult[T] = user.flatMap(transform)

    def readMeta: JsResult[DataRequest] = for {
      u <- user
      b <- body.validate[JsObject]
      meta <- DataRequest.fromJson(u, b)
    } yield meta

    request flatMap { req =>
      val message: JsResult[PimpMessage] = cmd flatMap {
        case VersionKey      => JsSuccess(GetVersion)
        case TrackKey        => body.validate[RangedTrack] orElse body.validate[GetTrack]
        case Cancel          => JsSuccess(CancelStream(req))
        case Meta            => body.validate[GetMeta]
        case Ping            => JsSuccess(PingMessage)
        case AuthenticateKey => body.validate[Authenticate]
        case RootFolderKey   => JsSuccess(RootFolder)
        case FolderKey       => body.validate[GetFolder]
        case SearchKey       => body.validate[Search]
        case PlaylistsGet    => user.map(u => GetPlaylists(u))
        case PlaylistGet     => withUser(u => (body \ Id).validate[PlaylistID].map(GetPlaylist(_, u)))
        case PlaylistSave =>
          withUser(u => (body \ PlaylistKey).validate[PlaylistSubmission].map(SavePlaylist(_, u)))
        case PlaylistDelete =>
          withUser(u => (body \ Id).validate[PlaylistID].map(DeletePlaylist(_, u)))
        case AlarmsKey  => JsSuccess(GetAlarms)
        case AlarmsEdit => body.validate[AlarmCommand].map(AlarmEdit.apply)
        case AlarmsAdd  => body.validate[AlarmCommand].map(AlarmAdd.apply)
        case Beam       => body.validate[BeamCommand]
        case StatusKey  => JsSuccess(GetStatus)
        case Recent     => readMeta.map(GetRecent.apply)
        case Popular    => readMeta.map(GetPopular.apply)
        case other      => JsError(s"Unknown JSON command: '$other' in '$json'.")
      }
      message.map(msg => CloudRequest(msg, req))
    }
  }

  def parseEvent(json: JsValue): JsResult[PimpMessage] = {
    val event = (json \ Cmd).validate[String].orElse((json \ EventKey).validate[String])
    val body = json \ Body
    event flatMap {
      case Registered =>
        body.validate[RegisteredMessage]
      case Player =>
        for {
          b <- body.validate[PlayerMessage]
          user <- (json \ UsernameKey).validate[Username]
        } yield PlaybackMessage(b, user)
      case Ping =>
        JsSuccess(PingMessage)
      case other =>
        JsError(s"Unknown JSON event: '$other' in '$json'.")
    }
  }

}
