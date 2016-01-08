package com.malliina.musicpimp.json

import com.malliina.audio.PlayerStates
import com.malliina.musicpimp.audio.TrackMeta
import com.malliina.musicpimp.json.JsonStrings._
import com.malliina.util.Log
import play.api.libs.json.Json._
import play.api.libs.json.{JsObject, JsValue, Json}

import scala.concurrent.duration.Duration

/**
 * @author Michael
 */
trait JsonMessages extends Log {
  val version = obj(VERSION -> com.malliina.musicpimp.BuildInfo.version)
  val noMedia = obj(STATE -> PlayerStates.NoMedia.toString)
  val unAuthorized = failure(ACCESS_DENIED)
  val databaseFailure = failure(DatabaseError)
  val genericFailure = failure(GenericError)
  val invalidParameter = failure(INVALID_PARAMETER)
  val invalidCredentials = failure(INVALID_CREDENTIALS)
  val invalidJson = failure(INVALID_JSON)
  val noFileInMultipart = failure(NO_FILE_IN_MULTIPART)
  val ping = event(PING)

  def failure(reason: String) = obj(REASON -> reason)

  def exception(e: Throwable) = failure(e.getMessage)

  def trackChanged(track: TrackMeta) =
    event(TRACK_CHANGED, TRACK -> toJson(track))

  // POS and POS_SECONDS are deprecated
  def timeUpdated(newTime: Duration) =
    event(TIME_UPDATED, POSITION -> newTime.toSeconds)

  def volumeChanged(newVolume: Int) =
    event(VOLUME_CHANGED, VOLUME -> newVolume)

  def muteToggled(newMute: Boolean) =
    event(MUTE_TOGGLED, MUTE -> newMute)

  def playlistModified(newPlaylist: Seq[TrackMeta]) =
    event(PLAYLIST_MODIFIED, PLAYLIST -> toJson(newPlaylist))

  def playlistIndexChanged(newIndex: Int) =
    event(PLAYLIST_INDEX_CHANGED, PLAYLIST_INDEX -> newIndex, PLAYLIST_INDEXv17v18 -> newIndex)

  def playStateChanged(newState: PlayerStates.Value) =
    event(PLAYSTATE_CHANGED, STATE -> newState.toString)

  def searchStatus(status: String) = event(SEARCH_STATUS, STATUS -> status)

  def withStatus(json: JsValue): JsValue = event(STATUS) ++ json.as[JsObject]

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject =
    obj(EVENT -> eventType) ++ obj(valuePairs: _*)

  def thanks = Json.obj(MSG -> Json.toJson(THANK_YOU))
}

object JsonMessages extends JsonMessages
