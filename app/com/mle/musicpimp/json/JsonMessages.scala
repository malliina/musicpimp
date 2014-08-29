package com.mle.musicpimp.json

import play.api.libs.json.{JsObject, Json}
import com.mle.musicpimp.audio.TrackMeta
import JsonStrings._
import Json._
import scala.concurrent.duration.Duration
import com.mle.util.Log
import com.mle.musicpimp.BuildInfo
import com.mle.audio.PlayerStates

/**
 * @author Michael
 */
trait JsonMessages extends Log {
  val Version = obj(VERSION -> BuildInfo.version)
  val NoMedia = obj(STATE -> PlayerStates.NoMedia.toString)
  val UnAuthorized = failure(ACCESS_DENIED)
  val InvalidParameter = failure(INVALID_PARAMETER)
  val InvalidJson = failure(INVALID_JSON)
  val NoFileInMultipart = failure(NO_FILE_IN_MULTIPART)
  val Ping = event(PING)

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

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject = {
    obj(EVENT -> eventType) ++ obj(valuePairs: _*)
  }

  def thanks = Json.obj(MSG -> Json.toJson(THANK_YOU))
}

object JsonMessages extends JsonMessages
