package com.mle.musicpimp.json

import play.api.libs.json.{JsObject, Json}
import com.mle.musicpimp.audio.PlayState
import JsonStrings._
import com.mle.musicpimp.library.TrackInfo
import Json._
import scala.concurrent.duration.Duration
import com.mle.audio.AudioImplicits._
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

  def failure(reason: String) = obj(REASON -> reason)

  def exception(e: Throwable) = failure(e.getMessage)

  def trackChanged(track: TrackInfo) =
    event(TRACK_CHANGED, TRACK -> toJson(track))

  // POS and POS_SECONDS are deprecated
  def timeUpdated(newTime: Duration) =
    event(TIME_UPDATED, POSITION -> newTime.toSeconds, POS -> newTime.readable, POS_SECONDS -> newTime.toSeconds)

  def volumeChanged(newVolume: Int) =
    event(VOLUME_CHANGED, VOLUME -> newVolume)

  def muteToggled(newMute: Boolean) =
    event(MUTE_TOGGLED, MUTE -> newMute)

  def playlistModified(newPlaylist: Seq[TrackInfo]) =
    event(PLAYLIST_MODIFIED, PLAYLIST -> toJson(newPlaylist))

  def playlistIndexChanged(newIndex: Int) =
    event(PLAYLIST_INDEX_CHANGED, PLAYLIST_INDEX -> newIndex)

  def playStateChanged(newState: PlayState.Value) =
    event(PLAYSTATE_CHANGED, STATE -> newState.toString)

  def event(eventType: String, valuePairs: (String, JsValueWrapper)*): JsObject = {
    obj(EVENT -> eventType) ++ obj(valuePairs: _*)
  }
}

object JsonMessages extends JsonMessages
