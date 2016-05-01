package com.malliina.musicpimp.audio

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.JsonHandlerBase.log
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.play.http.{AuthRequest, RequestInfo}
import play.api.Logger
import play.api.libs.json.{JsError, JsResult, JsSuccess, JsValue}

import scala.concurrent.duration.DurationDouble

trait JsonHandlerBase {

  def fulfillMessage(message: PlayerMessage, user: String): Unit

  def onJson(req: AuthRequest[JsValue]): Unit =
    onJson(req.body, RequestInfo(req.user, req))

  /** Handles messages sent by web players.
    */
  def onJson(msg: JsValue, req: RequestInfo): Unit = {
    val user = req.user
    log info s"User: $user from: ${req.request.remoteAddress} said: $msg"
    handleMessage(msg, user)
  }

  protected def handleMessage(msg: JsValue, user: String): Unit = {
    parse(msg)
      .map(fulfillMessage(_, user))
      .recoverTotal(err => log error s"Invalid JSON: $msg, error: $err")
  }

  def parse(msg: JsValue): JsResult[PlayerMessage] = {
    withCmd(msg)(cmd => cmd.command flatMap {
      case TIME_UPDATED =>
        cmd.doubleValue.map(pos => TimeUpdated(pos.seconds))
      case TRACK_CHANGED =>
        cmd.track.map(TrackChanged.apply)
      case VOLUME_CHANGED =>
        cmd.doubleValue.map(vol => VolumeChanged(vol.toInt))
      case PLAYLIST_INDEX_CHANGED =>
        cmd.intValue.map(PlaylistIndexChanged.apply)
      case PLAYSTATE_CHANGED =>
        implicit val reader = WebPlayerMessageHandler.playerStateReader
        (msg \ VALUE).validate[PlayerStates.Value].map(PlayStateChanged.apply)
      case MUTE_TOGGLED =>
        cmd.boolValue.map(MuteToggled.apply)
      case PLAY =>
        cmd.track.map(Play.apply)
      case ADD =>
        cmd.track.map(Add.apply)
      case REMOVE =>
        cmd.indexOrValue.map(Remove.apply)
      case RESUME =>
        JsSuccess(Resume)
      case STOP =>
        JsSuccess(Stop)
      case NEXT =>
        JsSuccess(Next)
      case PREV =>
        JsSuccess(Prev)
      case SKIP =>
        cmd.intValue.map(Skip.apply)
      case SEEK =>
        cmd.doubleValue.map(d => Seek(d.seconds))
      case MUTE =>
        cmd.boolValue.map(Mute.apply)
      case VOLUME =>
        cmd.doubleValue.map(d => Volume(d.toInt))
      case Insert =>
        for {
          track <- cmd.track
          index <- cmd.indexOrValue
        } yield InsertTrack(index, track)
      case Move =>
        for {
          from <- (msg \ From).validate[Int]
          to <- (msg \ To).validate[Int]
        } yield MoveTrack(from, to)
      case ADD_ITEMS =>
        JsSuccess(AddAll(cmd.foldersOrNil, cmd.tracksOrNil))
      case PLAY_ITEMS =>
        JsSuccess(PlayAll(cmd.foldersOrNil, cmd.tracksOrNil))
      case ResetPlaylist =>
        JsSuccess(ResetPlaylistMessage(cmd.index getOrElse BasePlaylist.NoPosition, cmd.tracksOrNil))
      case anythingElse =>
        JsError(s"Unknown message: $msg")
    })
  }

  def withCmd[T](json: JsValue)(f: JsonCmd => T): T =
    f(new JsonCmd(json))
}

object JsonHandlerBase {
  private val log = Logger(getClass)
}