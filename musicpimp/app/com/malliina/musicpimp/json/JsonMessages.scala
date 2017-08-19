package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.musicpimp.models.{BaseTrack, FailReason, Version}
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json, Writes}

import scala.concurrent.duration.Duration

object JsonMessages extends JsonMessages

trait JsonMessages extends CommonMessages {
  val version = Version(com.malliina.musicpimp.BuildInfo.version)
  val noMedia = obj(State -> PlayerStates.NoMedia.toString)
  val unAuthorized = FailReason(AccessDenied)
  val databaseFailure = FailReason(DatabaseError)
  val genericFailure = FailReason(GenericError)
  val invalidParameter = FailReason(InvalidParameter)
  val invalidCredentials = FailReason(InvalidCredentials)
  val invalidJson = FailReason(InvalidJson)
  val noFileInMultipart = FailReason(NoFileInMultipart)
  val ping = event(Ping)

  def exception(e: Throwable) =
    FailReason(e.getMessage)

  def trackChanged(track: BaseTrack)(implicit w: Writes[BaseTrack]) =
    event(TrackChanged, TrackKey -> toJson(track))

  // POS and POS_SECONDS are deprecated
  def timeUpdated(newTime: Duration) =
    event(TimeUpdated, Position -> newTime.toSeconds)

  def volumeChanged(newVolume: Int) =
    event(VolumeChanged, Volume -> newVolume)

  def muteToggled(newMute: Boolean) =
    event(MuteToggled, Mute -> newMute)

  def playlistModified(newPlaylist: Seq[BaseTrack])(implicit w: Writes[BaseTrack]) =
    event(PlaylistModified, Playlist -> toJson(newPlaylist))

  def playlistIndexChanged(newIndex: Int) =
    event(PlaylistIndexChanged, PlaylistIndex -> newIndex, PlaylistIndexv17v18 -> newIndex)

  def playStateChanged(newState: PlayerStates.Value) =
    event(PlaystateChanged, State -> newState.toString)

  def searchStatus(status: String) =
    event(SearchStatus, StatusKey -> status)

  def withStatus(json: JsValue): JsValue =
    event(StatusKey) ++ json.as[JsObject]

  def thanks =
    Json.obj(Msg -> Json.toJson(ThankYou))
}
