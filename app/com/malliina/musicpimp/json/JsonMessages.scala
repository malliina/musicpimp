package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.JsonStrings._
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import scala.concurrent.duration.Duration

object JsonMessages extends JsonMessages

trait JsonMessages {
  val version = obj(Version -> com.malliina.musicpimp.BuildInfo.version)
  val noMedia = obj(State -> PlayerStates.NoMedia.toString)
  val unAuthorized = failure(AccessDenied)
  val databaseFailure = failure(DatabaseError)
  val genericFailure = failure(GenericError)
  val invalidParameter = failure(InvalidParameter)
  val invalidCredentials = failure(InvalidCredentials)
  val invalidJson = failure(InvalidJson)
  val noFileInMultipart = failure(NoFileInMultipart)
  val ping = event(Ping)

  def failure(reason: String) =
    obj(Reason -> reason)

  def exception(e: Throwable) =
    failure(e.getMessage)

  def trackChanged(track: TrackMeta)(implicit w: Writes[TrackMeta]) =
    event(TrackChanged, TrackKey -> toJson(track))

  // POS and POS_SECONDS are deprecated
  def timeUpdated(newTime: Duration) =
    event(TimeUpdated, Position -> newTime.toSeconds)

  def volumeChanged(newVolume: Int) =
    event(VolumeChanged, Volume -> newVolume)

  def muteToggled(newMute: Boolean) =
    event(MuteToggled, Mute -> newMute)

  def playlistModified(newPlaylist: Seq[TrackMeta])(implicit w: Writes[TrackMeta]) =
    event(PlaylistModified, Playlist -> toJson(newPlaylist))

  def playlistIndexChanged(newIndex: Int) =
    event(PlaylistIndexChanged, PlaylistIndex -> newIndex, PlaylistIndexv17v18 -> newIndex)

  def playStateChanged(newState: PlayerStates.Value) =
    event(PlaystateChanged, State -> newState.toString)

  def searchStatus(status: String) =
    event(SearchStatus, Status -> status)

  def withStatus(json: JsValue): JsValue =
    event(Status) ++ json.as[JsObject]

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    obj(Event -> eventType) ++ obj(valuePairs: _*)

  def thanks =
    Json.obj(Msg -> Json.toJson(ThankYou))
}
