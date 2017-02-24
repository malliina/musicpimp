package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.JsonHandlerBase.log
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.RemoteInfo
import com.malliina.play.http.{CookiedRequest, RequestInfo}
import com.malliina.play.models.Username
import play.api.Logger
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}

import scala.concurrent.duration.DurationDouble

trait JsonHandlerBase {

  protected def fulfillMessage(message: PlayerMessage, request: RemoteInfo): Unit

  def onJson(req: CookiedRequest[JsValue, Username]): Unit =
    onJson(req.body, RequestInfo(req.user, req))

  /** Handles messages sent by web players.
    */
  def onJson(msg: JsValue, req: RequestInfo[Username]): Unit = {
    val user = req.user
    log info s"User '$user' from '${req.request.remoteAddress}' said: '$msg'."
    handleMessage(msg, RemoteInfo(req))
  }

  def handleMessage(msg: JsValue, request: RemoteInfo): Unit = {
    parse(msg)
      .map(fulfillMessage(_, request))
      .recoverTotal(err => log error s"Invalid JSON: '$msg', error: $err.")
  }

  def parse(msg: JsValue): JsResult[PlayerMessage] = {
    withCmd(msg)(cmd => cmd.command flatMap {
      case TimeUpdated =>
        cmd.doubleValue.map(pos => TimeUpdatedMsg(pos.seconds))
      case TrackChanged =>
        cmd.track.map(TrackChangedMsg.apply)
      case VolumeChanged =>
        cmd.doubleValue.map(vol => VolumeChangedMsg(vol.toInt))
      case PlaylistIndexChanged =>
        cmd.intValue.map(PlaylistIndexChangedMsg.apply)
      case PlaystateChanged =>
        implicit val reader = WebPlayerMessageHandler.playerStateReader
        (msg \ Value).validate[PlayerStates.Value].map(PlayStateChangedMsg.apply)
      case MuteToggled =>
        cmd.boolValue.map(MuteToggledMsg.apply)
      case Play =>
        cmd.track.map(PlayMsg.apply)
      case Add =>
        cmd.track.map(AddMsg.apply)
      case Remove =>
        cmd.indexOrValue.map(RemoveMsg.apply)
      case Resume =>
        JsSuccess(ResumeMsg)
      case Stop =>
        JsSuccess(StopMsg)
      case Next =>
        JsSuccess(NextMsg)
      case Prev =>
        JsSuccess(PrevMsg)
      case Skip =>
        cmd.intValue.map(SkipMsg.apply)
      case Seek =>
        cmd.doubleValue.map(d => SeekMsg(d.seconds))
      case Mute =>
        cmd.boolValue.map(MuteMsg.apply)
      case Volume =>
        cmd.doubleValue.map(d => VolumeMsg(d.toInt))
      case Insert =>
        for {
          track <- cmd.track
          index <- cmd.indexOrValue
        } yield InsertTrackMsg(index, track)
      case Move =>
        for {
          from <- (msg \ From).validate[Int]
          to <- (msg \ To).validate[Int]
        } yield MoveTrackMsg(from, to)
      case AddItems =>
        JsSuccess(AddAllMsg(cmd.foldersOrNil, cmd.tracksOrNil))
      case PlayItems =>
        JsSuccess(PlayAllMsg(cmd.foldersOrNil, cmd.tracksOrNil))
      case ResetPlaylist =>
        JsSuccess(ResetPlaylistMessage(cmd.index getOrElse BasePlaylist.NoPosition, cmd.tracksOrNil))
      case anythingElse =>
        JsError(s"Unknown message: '$msg'.")
    })
  }

  def withCmd[T](json: JsValue)(f: JsonCmd => T): T =
    f(new JsonCmd(json))
}

object JsonHandlerBase {
  private val log = Logger(getClass)
}